package com.usagetracker.app.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object TimeFormat {

    fun durationHm(ms: Long): String {
        if (ms <= 0L) return "0m"
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) - hours * 60L
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${TimeUnit.MILLISECONDS.toSeconds(ms)}s"
        }
    }

    fun durationShort(ms: Long): String {
        if (ms <= 0L) return "0m"
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) - hours * 60L
        return when {
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${TimeUnit.MILLISECONDS.toSeconds(ms)}s"
        }
    }

    fun clockTime(ms: Long, tz: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).apply { timeZone = tz }.format(Date(ms))
            .lowercase(Locale.getDefault())

    fun dateLabel(ms: Long, tz: TimeZone = TimeZone.getDefault()): String {
        val c = Calendar.getInstance(tz).apply {
            timeInMillis = ms
            val now = Calendar.getInstance(tz)
            if (get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) return "Today"
            if (get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1) return "Yesterday"
        }
        return SimpleDateFormat("EEE, d MMM", Locale.getDefault()).apply { timeZone = tz }
            .format(Date(ms))
    }

    fun fullDate(ms: Long, tz: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).apply { timeZone = tz }
            .format(Date(ms))

    fun monthYear(ms: Long, tz: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply { timeZone = tz }.format(Date(ms))

    fun relative(ms: Long): String =
        DateUtils.getRelativeTimeSpanString(ms, System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS).toString()
}
