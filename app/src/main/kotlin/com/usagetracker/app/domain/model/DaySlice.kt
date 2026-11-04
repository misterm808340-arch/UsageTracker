package com.usagetracker.app.domain.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Local-day boundary helpers. All "day" math uses the device's default timezone
 * because that is what the user perceives as a "day" — usage at 23:59 today
 * and 00:01 tomorrow belong to two different days.
 *
 * INFERRED — NOT VERIFIED: original DaySlice was not directly recoverable.
 */
object DaySlice {

    fun startOfDay(epochMs: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(tz).apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    fun endOfDay(epochMs: Long, tz: TimeZone = TimeZone.getDefault()): Long =
        startOfDay(epochMs, tz) + 24L * 60L * 60L * 1000L - 1L

    /** Inclusive start of the day for [epochDay] (days since 1970-01-01, UTC). */
    fun startOfEpochDay(epochDay: Int, tz: TimeZone = TimeZone.getDefault()): Long {
        val c = Calendar.getInstance(tz).apply {
            clear()
            set(Calendar.YEAR, 1970)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, epochDay)
        }
        return c.timeInMillis
    }

    /** Hour-of-day [0, 23] for the given epoch ms in the given timezone. */
    fun hourOfDay(epochMs: Long, tz: TimeZone = TimeZone.getDefault()): Int {
        val c = Calendar.getInstance(tz).apply { timeInMillis = epochMs }
        return c.get(Calendar.HOUR_OF_DAY)
    }
}
