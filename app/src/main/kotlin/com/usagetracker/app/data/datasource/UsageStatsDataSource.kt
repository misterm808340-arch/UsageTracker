package com.usagetracker.app.data.datasource

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.usagetracker.app.domain.model.UsageEventRecord
import com.usagetracker.app.domain.model.UsageBucket

/**
 * Wraps [UsageStatsManager] and parses raw [UsageEvents] into normalized
 * [UsageEventRecord] and [UsageBucket] lists.
 *
 * INFERRED — NOT VERIFIED.
 */
class UsageStatsDataSource(private val context: Context) {

    private val usm: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun hasUsageAccess(): Boolean {
        return try {
            val now = System.currentTimeMillis()
            // queryEvents returns an empty Events if permission is missing; check via AppOps.
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (t: Throwable) { false }
    }

    /** Raw [UsageStats] list as the system returns it (often noisy). */
    fun queryUsageStats(startMs: Long, endMs: Long): List<UsageStats> {
        val m = usm ?: return emptyList()
        return try {
            m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startMs, endMs) ?: emptyList()
        } catch (t: SecurityException) { emptyList() }
    }

    /** Normalized event stream for [startMs, endMs]. */
    fun queryEvents(startMs: Long, endMs: Long): List<UsageEventRecord> {
        val m = usm ?: return emptyList()
        val events = try {
            m.queryEvents(startMs, endMs)
        } catch (t: SecurityException) { return emptyList() }
        val out = ArrayList<UsageEventRecord>(128)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            out.add(
                UsageEventRecord(
                    packageName = pkg,
                    timestampMs = event.timeStamp,
                    eventType = UsageEventRecord.Type.fromUsageEventsEventType(event.eventType),
                )
            )
        }
        return out
    }

    /**
     * Cheap foreground buckets — used when event query is unavailable.
     * Falls back to [UsageStats.getLastTimeUsed] / [UsageStats.getTotalTimeInForeground].
     */
    fun queryBucketsFromStats(startMs: Long, endMs: Long): List<UsageBucket> {
        return queryUsageStats(startMs, endMs).mapNotNull { s ->
            if (s.totalTimeInForeground <= 0L) return@mapNotNull null
            UsageBucket(
                packageName = s.packageName,
                startTimeMs = s.firstTimeStamp,
                endTimeMs = s.lastTimeStamp,
            )
        }
    }
}
