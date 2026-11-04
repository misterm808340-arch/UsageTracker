package com.usagetracker.app.data.repository

import android.content.Context
import com.usagetracker.app.data.datasource.InstalledAppsDataSource
import com.usagetracker.app.data.datasource.UsageStatsDataSource
import com.usagetracker.app.data.local.UsageDatabase
import com.usagetracker.app.data.local.entity.AppInfoEntity
import com.usagetracker.app.data.local.entity.DailyUsageEntity
import com.usagetracker.app.data.local.entity.UsageSessionEntity
import com.usagetracker.app.domain.model.AppRanking
import com.usagetracker.app.domain.model.AppUsageInfo
import com.usagetracker.app.domain.model.DailyUsage
import com.usagetracker.app.domain.model.DaySlice
import com.usagetracker.app.domain.model.EpochDay
import com.usagetracker.app.domain.model.HourlyUsage
import com.usagetracker.app.domain.model.TimelineEntry
import com.usagetracker.app.domain.model.UsageBucket
import com.usagetracker.app.domain.usage.SessionDetector
import com.usagetracker.app.domain.usage.UsageAggregator
import com.usagetracker.app.domain.util.SystemPackageFilter
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Coordinates UsageStatsManager → SessionDetector → UsageAggregator → Room.
 *
 * v1.2.2-fix: all UsageStatsManager / PackageManager / aggregation work now runs
 * on Dispatchers.IO (previously it blocked the main thread), installed-app scans
 * are throttled, and range rankings reuse the Room-persisted sessions instead of
 * re-querying weeks of UsageEvents on every screen refresh.
 */
class UsageRepositoryImpl(
    private val context: Context,
    private val db: UsageDatabase,
    private val usageStats: UsageStatsDataSource,
    private val installedApps: InstalledAppsDataSource,
) {

    private val syncMutex = Mutex()
    private var lastInstalledScanMs = 0L
    private var lastSyncRangeKey: Pair<Long, Long> = Pair(0L, 0L)

    /** Throttled: full PackageManager scan at most once every 5 minutes. */
    suspend fun refreshInstalledApps(includeSystem: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - lastInstalledScanMs < INSTALLED_SCAN_THROTTLE_MS) return@withContext
        lastInstalledScanMs = now
        val list = installedApps.listInstalled(includeSystem = true)
        val entities = list.map { installedApps.toEntity(it, now) }
        db.appInfoDao().upsert(entities)
        db.appInfoDao().deleteMissing(list.map { it.packageName })
    }

    suspend fun appLabels(): Map<String, String> = withContext(Dispatchers.IO) {
        db.appInfoDao().getAll().associate { it.packageName to it.label }
    }

    suspend fun appSystemFlags(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        db.appInfoDao().getAll().associate { it.packageName to it.isSystem }
    }

    fun observeApps(): Flow<List<AppInfoEntity>> = db.appInfoDao().observeAll()

    /**
     * Pull fresh events from [android.app.usage.UsageStatsManager] for the requested
     * range, run SessionDetector, persist sessions, recompute daily aggregates.
     * Runs entirely on IO; guarded by a mutex so parallel callers don't duplicate work.
     */
    suspend fun syncRange(startMs: Long, endMs: Long): List<UsageBucket> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            // Skip if an identical range was synced in the last few seconds.
            val now = System.currentTimeMillis()
            val key = Pair(startMs, endMs)
            if (key == lastSyncRangeKey && now - endMs < SYNC_DEDUPE_WINDOW_MS) {
                return@withLock emptyList()
            }
            lastSyncRangeKey = key

            val events = usageStats.queryEvents(startMs, endMs)
            val buckets = SessionDetector.detect(events, startMs, endMs)
            if (buckets.isNotEmpty()) {
                val sessionEntities = UsageAggregator.toSessionEntities(buckets)
                db.usageSessionDao().insertAll(sessionEntities)
                // Rebuild touched days — group once (O(N)), then aggregate per day.
                val tz = java.util.TimeZone.getDefault()
                val byDay: Map<Int, List<UsageBucket>> =
                    buckets.groupBy { EpochDay.fromEpochMs(it.startTimeMs, tz) }
                val touchedDays = byDay.keys
                for (day in touchedDays) {
                    val dayBuckets = byDay.getValue(day)
                    val dailyEntities = UsageAggregator.toDailyEntities(dayBuckets, day, now)
                    db.dailyUsageDao().upsertAll(dailyEntities)
                }
            }
            buckets
        }
    }

    /**
     * One sync for "today so far" that returns BOTH the app ranking and the hourly
     * histogram — the old code synced the same range twice per refresh.
     */
    suspend fun todayOverview(includeSystem: Boolean): Pair<AppRanking, List<HourlyUsage>> =
        withContext(Dispatchers.IO) {
            val tz = java.util.TimeZone.getDefault()
            val dayStart = DaySlice.startOfDay(System.currentTimeMillis(), tz)
            val dayEnd = System.currentTimeMillis()
            val buckets = syncRange(dayStart, dayEnd)
            val filtered = filterBuckets(buckets, includeSystem)
            val labels = appLabels()
            val sys = appSystemFlags()
            val ranking = UsageAggregator.aggregateAppUsage(filtered, labels, sys, dayStart, dayEnd)
            val hourly = UsageAggregator.hourlyHistogram(filtered, EpochDay.today(tz))
            Pair(ranking, hourly)
        }

    /** Aggregate for Overview "Today's usage" card. */
    suspend fun todaysRanking(includeSystem: Boolean): AppRanking =
        todayOverview(includeSystem).first

    suspend fun rangeRanking(startMs: Long, endMs: Long, includeSystem: Boolean): AppRanking =
        withContext(Dispatchers.IO) {
            val tz = java.util.TimeZone.getDefault()
            val todayStart = DaySlice.startOfDay(System.currentTimeMillis(), tz)
            if (endMs >= todayStart) {
                // Range includes today — today needs a live event sync.
                syncRange(todayStart, endMs)
            }
            if (startMs < todayStart) {
                // History beyond today: one-time backfill per process (the periodic
                // UsageSyncWorker only keeps the trailing 24 h fresh).
                backfillIfNeeded(startMs)
            }
            // Rankings come from the persisted sessions (cheap, indexed).
            val fromDay = EpochDay.fromEpochMs(startMs, tz)
            val toDay = EpochDay.fromEpochMs(endMs, tz)
            val sessions = db.usageSessionDao().forDayRange(fromDay, toDay)
                .filter { it.startTimeMs in startMs..endMs }
            val labels = appLabels()
            val sys = appSystemFlags()
            val items = sessions.groupBy { it.packageName }.map { (pkg, list) ->
                AppUsageInfo(
                    packageName = pkg,
                    label = labels[pkg] ?: pkg,
                    totalDurationMs = list.sumOf { it.durationMs },
                    sessionCount = list.size,
                    openCount = list.size,
                    firstUsedMs = list.minOfOrNull { it.startTimeMs },
                    lastUsedMs = list.maxOfOrNull { it.endTimeMs },
                    isSystem = sys[pkg] ?: false,
                )
            }.filter {
                SystemPackageFilter.isUserFacing(it.packageName, it.isSystem, includeSystem)
            }.sortedByDescending { it.totalDurationMs }
            AppRanking(items, items.sumOf { it.totalDurationMs }, startMs, endMs)
        }

    suspend fun hourlyForToday(): List<HourlyUsage> {
        val tz = java.util.TimeZone.getDefault()
        val day = EpochDay.today(tz)
        val dayStart = EpochDay.toEpochMs(day, tz)
        val now = System.currentTimeMillis()
        val buckets = syncRange(dayStart, now)
        return UsageAggregator.hourlyHistogram(buckets, day)
    }

    /** Per-day totals computed from persisted daily_usage rows (no event re-query). */
    suspend fun dailyTotalsForRange(epochDayFrom: Int, epochDayTo: Int): List<DailyUsage> =
        withContext(Dispatchers.IO) {
            db.dailyUsageDao().forDayRange(epochDayFrom, epochDayTo).map {
                DailyUsage(
                    epochDay = it.epochDay,
                    packageName = it.packageName,
                    totalDurationMs = it.totalDurationMs,
                    sessionCount = it.sessionCount,
                    openCount = it.openCount,
                )
            }
        }

    suspend fun dailyForRange(epochDayFrom: Int, epochDayTo: Int): List<DailyUsage> =
        withContext(Dispatchers.IO) {
            val sessions = db.usageSessionDao().forDayRange(epochDayFrom, epochDayTo)
            UsageAggregator.dailyFromSessions(sessions)
        }

    /**
     * Timeline for one day — NEWEST FIRST (the UI used to render only the first 40
     * chronological entries, hiding everything after early morning). System apps
     * are filtered so launcher/systemui noise doesn't flood the list.
     */
    suspend fun timelineForDay(day: Int, includeSystem: Boolean = false, limit: Int = 60): List<TimelineEntry> =
        withContext(Dispatchers.IO) {
            val sessions = db.usageSessionDao().forDay(day)
            val labels = appLabels()
            val sys = appSystemFlags()
            sessions.asSequence()
                .filter { SystemPackageFilter.isUserFacing(it.packageName, sys[it.packageName] ?: false, includeSystem) }
                .sortedByDescending { it.startTimeMs }
                .take(limit)
                .map {
                    TimelineEntry(
                        packageName = it.packageName,
                        label = labels[it.packageName] ?: it.packageName,
                        startTimeMs = it.startTimeMs,
                        endTimeMs = it.endTimeMs,
                        durationMs = it.durationMs,
                    )
                }
                .toList()
        }

    suspend fun timelineForRange(startMs: Long, endMs: Long): List<TimelineEntry> =
        withContext(Dispatchers.IO) {
            val sessions = db.usageSessionDao().range(startMs, endMs)
            val labels = appLabels()
            UsageAggregator.timelineFromSessions(sessions, labels)
        }

    suspend fun perAppHistory(packageName: String, fromDay: Int, toDay: Int): List<DailyUsage> =
        withContext(Dispatchers.IO) {
            db.usageSessionDao().forDayRange(fromDay, toDay)
                .filter { it.packageName == packageName }
                .let { UsageAggregator.dailyFromSessions(it) }
        }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        db.usageSessionDao().clear()
        db.dailyUsageDao().clear()
        db.appInfoDao().clear()
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        db.usageSessionDao().clear()
        db.dailyUsageDao().clear()
    }

    private suspend fun filterBuckets(buckets: List<UsageBucket>, includeSystem: Boolean): List<UsageBucket> {
        val sys = appSystemFlags()
        return buckets.filter {
            SystemPackageFilter.isUserFacing(it.packageName, sys[it.packageName] ?: false, includeSystem)
        }
    }

    /**
     * One-time per-process backfill of historical usage events, so 7/30-day
     * views work on a fresh install. Guarded by a mutex + flag: repeated
     * range requests never re-query the same history twice.
     */
    private suspend fun backfillIfNeeded(rangeStartMs: Long) {
        backfillMutex.withLock {
            if (hasBackfilled) return
            hasBackfilled = true
            syncRange(rangeStartMs, System.currentTimeMillis())
        }
    }

    fun hasUsageAccess(): Boolean = usageStats.hasUsageAccess()

    companion object {
        private const val INSTALLED_SCAN_THROTTLE_MS = 5L * 60L * 1000L
        private const val SYNC_DEDUPE_WINDOW_MS = 3_000L
        private val backfillMutex = Mutex()
        @Volatile private var hasBackfilled = false
    }
}
