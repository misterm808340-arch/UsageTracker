package com.usagetracker.app.domain.util

import com.usagetracker.app.domain.model.AppUsageInfo
import com.usagetracker.app.domain.model.DailyUsage
import com.usagetracker.app.domain.model.HourlyUsage
import com.usagetracker.app.domain.model.TimelineEntry
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds CSV exports. Two sections:
 *   1) Per-app daily totals (one row per day × app)
 *   2) Timeline of sessions (one row per session)
 *
 * INFERRED — NOT VERIFIED.
 */
object CsvBuilder {

    private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
    private val DATE = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    fun build(
        writer: Writer,
        dailyByApp: Map<String, List<DailyUsage>>,   // packageName -> days
        timeline: List<TimelineEntry>,
        appLabels: Map<String, String>,
    ) {
        writer.write("# Usage Tracker export — generated ${ISO.format(Date())}\n")
        writer.write("# All times in device timezone (${TimeZone.getDefault().id})\n\n")

        writer.write("section,date,package,label,duration_ms,session_count,open_count,start_iso,end_iso\n")

        // Daily totals
        for ((pkg, days) in dailyByApp) {
            val label = appLabels[pkg] ?: pkg
            for (d in days) {
                val date = DATE.format(Date(com.usagetracker.app.domain.model.EpochDay.toEpochMs(d.epochDay)))
                writer.write("daily,")
                writer.write(escape(date)); writer.write(",")
                writer.write(escape(pkg)); writer.write(",")
                writer.write(escape(label)); writer.write(",")
                writer.write(d.totalDurationMs.toString()); writer.write(",")
                writer.write(d.sessionCount.toString()); writer.write(",")
                writer.write(d.openCount.toString()); writer.write(",")
                writer.write(",")  // start_iso blank for daily
                writer.write("\n")
            }
        }

        // Timeline
        for (t in timeline) {
            val label = appLabels[t.packageName] ?: t.packageName
            writer.write("timeline,")
            writer.write(escape(DATE.format(Date(t.startTimeMs)))); writer.write(",")
            writer.write(escape(t.packageName)); writer.write(",")
            writer.write(escape(label)); writer.write(",")
            writer.write(t.durationMs.toString()); writer.write(",")
            writer.write("1,1,")
            writer.write(escape(ISO.format(Date(t.startTimeMs)))); writer.write(",")
            writer.write(escape(ISO.format(Date(t.endTimeMs)))); writer.write("\n")
        }
        writer.flush()
    }

    private fun escape(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
}
