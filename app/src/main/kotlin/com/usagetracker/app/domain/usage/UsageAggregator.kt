package com.usagetracker.app.domain.usage

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
import com.usagetracker.app.domain.model.UsageEventRecord

/**
 * Pure aggregation logic — no Android imports beyond [java.util.TimeZone].
 *
 * INFERRED — NOT VERIFIED.
 */
object UsageAggregator {

    /** Per-app aggregate for [buckets]. */
    fun aggregateAppUsage(
        buckets: List<UsageBucket>,
        appLabels: Map<String, String>,
        systemFlags: Map<String, Boolean>,
        rangeStartMs: Long,
        rangeEndMs: Long,
    ): AppRanking {
        val byPkg = HashMap<String, ArrayList<UsageBucket>>()
        for (b in buckets) byPkg.getOrPut(b.packageName) { ArrayList() }.add(b)
        val items = byPkg.entries.map { (pkg, list) ->
            val sorted = list.sortedBy { it.startTimeMs }
            val total = sorted.sumOf { it.durationMs }
            AppUsageInfo(
                packageName = pkg,
                label = appLabels[pkg] ?: pkg,
                totalDurationMs = total,
                sessionCount = sorted.size,
                openCount = sorted.size,            // open-count ≈ session-count when event-based
                firstUsedMs = sorted.firstOrNull()?.startTimeMs,
                lastUsedMs = sorted.lastOrNull()?.endTimeMs,
                isSystem = systemFlags[pkg] ?: false,
            )
        }.sortedByDescending { it.totalDurationMs }
        val total = items.sumOf { it.totalDurationMs }
        return AppRanking(items, total, rangeStartMs, rangeEndMs)
    }

    /** Hour-of-day histogram for [buckets] that fall on the given epoch day. */
    fun hourlyHistogram(buckets: List<UsageBucket>, epochDay: Int): List<HourlyUsage> {
        val tz = java.util.TimeZone.getDefault()
        val dayStart = EpochDay.toEpochMs(epochDay, tz)
        val dayEnd = dayStart + 24L * 60L * 60L * 1000L - 1L
        val totals = LongArray(24)
        for (b in buckets) {
            if (b.endTimeMs < dayStart || b.startTimeMs > dayEnd) continue
            val clippedStart = maxOf(b.startTimeMs, dayStart)
            val clippedEnd = minOf(b.endTimeMs, dayEnd)
            if (clippedEnd <= clippedStart) continue
            val startHour = DaySlice.hourOfDay(clippedStart, tz)
            val endHour = DaySlice.hourOfDay(clippedEnd - 1, tz)
            if (startHour == endHour) {
                totals[startHour] += (clippedEnd - clippedStart)
            } else {
                totals[startHour] += (DaySlice.startOfDay(clippedStart, tz)
                    + (startHour + 1) * 3_600_000L - clippedStart)
                for (h in (startHour + 1) until endHour) totals[h] += 3_600_000L
                totals[endHour] += (clippedEnd - (DaySlice.startOfDay(clippedStart, tz) + endHour * 3_600_000L))
            }
        }
        return (0..23).map { HourlyUsage(it, totals[it]) }
    }

    /** Build [UsageSessionEntity] rows for storage. */
    fun toSessionEntities(buckets: List<UsageBucket>): List<UsageSessionEntity> {
        val tz = java.util.TimeZone.getDefault()
        return buckets.map { b ->
            UsageSessionEntity(
                packageName = b.packageName,
                startTimeMs = b.startTimeMs,
                endTimeMs = b.endTimeMs,
                durationMs = b.durationMs,
                epochDay = EpochDay.fromEpochMs(b.startTimeMs, tz),
                hourOfDay = DaySlice.hourOfDay(b.startTimeMs, tz),
            )
        }
    }

    /** Build [DailyUsageEntity] rows for a given epoch day. */
    fun toDailyEntities(buckets: List<UsageBucket>, day: Int, now: Long): List<DailyUsageEntity> {
        val byPkg = buckets.groupBy { it.packageName }
        return byPkg.entries.map { (pkg, list) ->
            DailyUsageEntity(
                epochDay = day,
                packageName = pkg,
                totalDurationMs = list.sumOf { it.durationMs },
                sessionCount = list.size,
                openCount = list.size,
                updatedAtMs = now,
            )
        }
    }

    /** Daily aggregates from session entities. */
    fun dailyFromSessions(sessions: List<UsageSessionEntity>): List<DailyUsage> =
        sessions.groupBy { it.epochDay }
            .flatMap { (day, list) ->
                list.groupBy { it.packageName }
                    .map { (pkg, perApp) ->
                        DailyUsage(
                            epochDay = day,
                            packageName = pkg,
                            totalDurationMs = perApp.sumOf { it.durationMs },
                            sessionCount = perApp.size,
                            openCount = perApp.size,
                        )
                    }
            }

    /** Timeline from session entities — chronological. */
    fun timelineFromSessions(
        sessions: List<UsageSessionEntity>,
        appLabels: Map<String, String>,
    ): List<TimelineEntry> = sessions.map {
        TimelineEntry(
            packageName = it.packageName,
            label = appLabels[it.packageName] ?: it.packageName,
            startTimeMs = it.startTimeMs,
            endTimeMs = it.endTimeMs,
            durationMs = it.durationMs,
        )
    }.sortedBy { it.startTimeMs }

    /**
     * Estimate open count from raw events when SessionDetector cannot be used
     * (e.g. for ranges beyond the event-history window).
     */
    fun estimateOpenCounts(events: List<UsageEventRecord>): Map<String, Int> {
        val counts = HashMap<String, Int>()
        for (e in events) {
            if (e.eventType != UsageEventRecord.Type.ACTIVITY_RESUMED) continue
            counts[e.packageName] = (counts[e.packageName] ?: 0) + 1
        }
        return counts
    }
}
