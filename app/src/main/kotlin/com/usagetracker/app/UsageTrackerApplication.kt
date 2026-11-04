package com.usagetracker.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.usagetracker.app.data.datasource.AppIconCache
import com.usagetracker.app.data.datasource.InstalledAppsDataSource
import com.usagetracker.app.data.datasource.UsageStatsDataSource
import com.usagetracker.app.data.local.UsageDatabase
import com.usagetracker.app.data.repository.BlockerRepositoryImpl
import com.usagetracker.app.data.repository.SettingsRepositoryImpl
import com.usagetracker.app.data.repository.UsageRepositoryImpl
import com.usagetracker.app.data.datastore.SettingsDataStore
import com.usagetracker.app.ads.AdMob
import com.usagetracker.app.di.AppViewModelFactory
import com.usagetracker.app.work.UsageSyncWorker
import java.util.concurrent.TimeUnit

class UsageTrackerApplication : Application(), Configuration.Provider {

    lateinit var db: UsageDatabase
        private set
    lateinit var usageStats: UsageStatsDataSource
        private set
    lateinit var installedApps: InstalledAppsDataSource
        private set
    lateinit var usageRepo: UsageRepositoryImpl
        private set
    lateinit var blockerRepo: BlockerRepositoryImpl
        private set
    lateinit var settingsRepo: SettingsRepositoryImpl
        private set
    lateinit var viewModelFactory: AppViewModelFactory
        private set

    override fun onCreate() {
        super.onCreate()

        db = UsageDatabase.get(this)
        usageStats = UsageStatsDataSource(this)
        installedApps = InstalledAppsDataSource(this)
        usageRepo = UsageRepositoryImpl(this, db, usageStats, installedApps)
        blockerRepo = BlockerRepositoryImpl(this, db)
        val settingsStore = SettingsDataStore(this)
        settingsRepo = SettingsRepositoryImpl(this, settingsStore, usageStats)
        viewModelFactory = AppViewModelFactory(this, usageRepo, blockerRepo, settingsRepo)

        AppIconCache.get(this)  // eager init

        createNotificationChannels()

        // AdMob: initialize on a background thread (the SDK helper handles threading).
        AdMob.initialize(this)

        // Schedule periodic usage sync (every ~6 hours)
        WorkManager.initialize(this, workManagerConfiguration)
        val syncReq = PeriodicWorkRequestBuilder<UsageSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UsageSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncReq
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                "blocker",
                getString(R.string.blocker_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.blocker_channel_desc)
                setShowBadge(false)
            }
        )
    }
}
