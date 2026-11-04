package com.usagetracker.app.domain.usage

import com.usagetracker.app.domain.model.UsageBucket
import com.usagetracker.app.domain.model.UsageEventRecord

/**
 * Turns a flat stream of [UsageEventRecord]s into foreground [UsageBucket]s.
 *
 * Rules (mirror common Android usage-engine behavior):
 *   - A bucket starts on ACTIVITY_RESUMED.
 *   - It ends on the next ACTIVITY_PAUSED for the SAME package, OR on the
 *     next ACTIVITY_RESUMED for a DIFFERENT package (whichever first).
 *   - Buckets with duration < MIN_DURATION_MS are dropped (short UI flickers
 *     such as notification taps, permission dialogs, etc.).
 *   - Buckets that span a day boundary are split at midnight local time, so
 *     the daily aggregate belongs to the correct day.
 *   - Duplicate consecutive RESUMED events for the same package are merged.
 *
 * INFERRED — NOT VERIFIED.
 */
object SessionDetector {

    private const val MIN_DURATION_MS = 1_000L        // 1 second
    private const val MAX_SINGLE_BUCKET_MS = 6L * 60L * 60L * 1000L  // 6h cap (anti-stuck)

    fun detect(events: List<UsageEventRecord>, rangeStartMs: Long, rangeEndMs: Long): List<UsageBucket> {
        if (events.isEmpty()) return emptyList()

        val sorted = events.sortedBy { it.timestampMs }
        val buckets = ArrayList<UsageBucket>(sorted.size / 2 + 1)

        var currentPkg: String? = null
        var currentStart: Long = -1L

        for (ev in sorted) {
            when (ev.eventType) {
                UsageEventRecord.Type.ACTIVITY_RESUMED -> {
                    if (currentPkg != null && currentPkg != ev.packageName) {
                        // Implicit pause for the previous pkg — close its bucket.
                        closeBucket(buckets, currentPkg, currentStart, ev.timestampMs)
                    }
                    if (currentPkg != ev.packageName) {
                        currentPkg = ev.packageName
                        currentStart = ev.timestampMs
                    }
                    // Duplicate RESUMED for same pkg: ignore.
                }
                UsageEventRecord.Type.ACTIVITY_PAUSED -> {
                    if (currentPkg == ev.packageName && currentStart >= 0L) {
                        closeBucket(buckets, currentPkg, currentStart, ev.timestampMs)
                        currentPkg = null
                        currentStart = -1L
                    }
                }
                else -> { /* ignore */ }
            }
        }
        // Trailing open bucket — cap at rangeEndMs.
        if (currentPkg != null && currentStart >= 0L) {
            closeBucket(buckets, currentPkg, currentStart, rangeEndMs.coerceAtMost(System.currentTimeMillis()))
        }

        return buckets
    }

    private fun closeBucket(
        out: MutableList<UsageBucket>,
        pkg: String,
        start: Long,
        end: Long,
    ) {
        val safeEnd = end.coerceAtLeast(start + 1L)
        val duration = safeEnd - start
        if (duration < MIN_DURATION_MS) return
        val cappedEnd = if (duration > MAX_SINGLE_BUCKET_MS) start + MAX_SINGLE_BUCKET_MS else safeEnd
        // Split across day boundaries (local tz).
        splitAcrossDays(pkg, start, cappedEnd, out)
    }

    private fun splitAcrossDays(pkg: String, start: Long, end: Long, out: MutableList<UsageBucket>) {
        val tz = java.util.TimeZone.getDefault()
        var s = start
        while (s < end) {
            val dayEnd = com.usagetracker.app.domain.model.DaySlice.endOfDay(s, tz) + 1L
            val chunkEnd = minOf(end, dayEnd)
            out.add(UsageBucket(pkg, s, chunkEnd))
            s = chunkEnd
        }
    }
}
