package com.usagetracker.app.domain.model

/**
 * Aggregated usage for a single app over a time range.
 *
 * INFERRED — NOT VERIFIED: the original app's exact UI-facing model is unknown.
 */
data class AppUsageInfo(
    val packageName: String,
    val label: String,
    val totalDurationMs: Long,
    val sessionCount: Int,
    val openCount: Int,
    val firstUsedMs: Long?,
    val lastUsedMs: Long?,
    val isSystem: Boolean,
) {
    val sharePercentOf: (Long) -> Float = { total ->
        if (total <= 0L) 0f else (totalDurationMs.toFloat() / total.toFloat()) * 100f
    }
}

/**
 * A ranked list of apps by usage duration.
 */
data class AppRanking(
    val items: List<AppUsageInfo>,
    val totalDurationMs: Long,
    val rangeStartMs: Long,
    val rangeEndMs: Long,
)

/**
 * A chronological timeline entry — one row in the Overview / History timeline.
 */
data class TimelineEntry(
    val packageName: String,
    val label: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
)

/**
 * Daily aggregate — one row in the daily_usage table.
 */
data class DailyUsage(
    val epochDay: Int,
    val packageName: String,
    val totalDurationMs: Long,
    val sessionCount: Int,
    val openCount: Int,
)

/**
 * Hourly histogram for the current day (24 buckets).
 */
data class HourlyUsage(
    val hourOfDay: Int,        // 0..23
    val totalDurationMs: Long,
)
