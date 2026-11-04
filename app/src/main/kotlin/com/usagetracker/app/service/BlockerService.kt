package com.usagetracker.app.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.usagetracker.app.R
import com.usagetracker.app.UsageTrackerApplication
import com.usagetracker.app.data.local.entity.BlockedAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that overlays user-selected apps when they are detected
 * in the foreground. Re-arms itself on boot via [BootReceiver].
 *
 * v1.2.2-fix: foreground detection rewritten. The old implementation polled
 * `queryUsageStats(INTERVAL_BEST, now-5s, now)`, which returns bucketed stats
 * and is almost always EMPTY for a 5-second window — so the overlay never
 * appeared. We now detect the foreground app from UsageEvents: the LAST
 * ACTIVITY_RESUMED event in the trailing ~10 s window is the app on screen.
 * All blocked packages are watched (previously only the first row), and the
 * binder call runs off the main thread.
 *
 * Limitations honestly acknowledged:
 *   - SYSTEM_ALERT_WINDOW is required to draw the overlay.
 *   - We cannot kill or force-stop the target app — Android forbids it. The
 *     overlay covers it and the user presses Back/Home.
 */
class BlockerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wm: WindowManager? = null
    private var overlay: View? = null
    private var overlayPkg: String? = null
    private var watcherJob: Job? = null
    private var expiryJobs = mutableMapOf<String, Job>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        observeBlocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun observeBlocks() {
        val app = applicationContext as UsageTrackerApplication
        scope.launch {
            app.blockerRepo.observeBlocked().collect { blocked ->
                expiryJobs.values.forEach { it.cancel() }
                expiryJobs.clear()
                watcherJob?.cancel()
                removeOverlay()

                app.blockerRepo.expireDue()
                val now = System.currentTimeMillis()
                val active = blocked.filter { it.expiresAtMs > now }
                if (active.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collect
                }
                startWatcher(active.map { it.packageName }.toSet())
                active.forEach { block -> startExpiryTimer(block) }
            }
        }
    }

    private fun startWatcher(packages: Set<String>) {
        watcherJob?.cancel()
        watcherJob = scope.launch {
            while (isActive) {
                val fg = currentForegroundPackage()
                if (fg != null && fg in packages) {
                    showOverlay(fg)
                } else {
                    removeOverlay()
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun startExpiryTimer(block: BlockedAppEntity) {
        expiryJobs[block.packageName] = ioScope.launch {
            val ms = (block.expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(ms)
            (applicationContext as UsageTrackerApplication).blockerRepo.unblock(block.packageName)
        }
    }

    /**
     * Reliable foreground detection: replay the last few seconds of UsageEvents
     * and return the package of the most recent ACTIVITY_RESUMED. The binder
     * call runs on Dispatchers.IO.
     */
    private suspend fun currentForegroundPackage(): String? = withContext(Dispatchers.IO) {
        try {
            val usm = getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - FOREGROUND_WINDOW_MS, now)
            val event = android.app.usage.UsageEvents.Event()
            var lastPkg: String? = null
            var lastTs = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                // 1 == ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
                if (event.eventType == 1 && event.timeStamp >= lastTs) {
                    lastTs = event.timeStamp
                    lastPkg = event.packageName
                }
            }
            if (lastPkg != null && now - lastTs < FOREGROUND_STALE_MS) lastPkg else null
        } catch (t: Throwable) {
            null
        }
    }

    private fun showOverlay(pkg: String) {
        if (overlay != null && overlayPkg == pkg) return
        removeOverlay()
        if (!Settings.canDrawOverlays(this)) return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.CENTER
        }
        val view = (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.blocker_overlay, null)
        view.findViewById<TextView>(R.id.blocker_overlay_text).text =
            getString(R.string.blocker_overlay_text)
        view.findViewById<Button>(R.id.blocker_overlay_close).apply {
            setOnClickListener {
                val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(home)
            }
        }
        runCatching {
            wm?.addView(view, params)
            overlay = view
            overlayPkg = pkg
        }
    }

    private fun removeOverlay() {
        overlay?.let { runCatching { wm?.removeView(it) } }
        overlay = null
        overlayPkg = null
    }

    private fun buildNotification(): android.app.Notification {
        val channelId = "blocker"
        return androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.blocker_notification_text))
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        ioScope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 4201
        const val EXTRA_PKG = "pkg"
        const val EXTRA_DURATION_MS = "duration_ms"
        private const val POLL_INTERVAL_MS = 900L
        private const val FOREGROUND_WINDOW_MS = 12_000L
        private const val FOREGROUND_STALE_MS = 10_000L
    }
}
