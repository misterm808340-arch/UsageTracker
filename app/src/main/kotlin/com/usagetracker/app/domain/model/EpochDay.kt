package com.usagetracker.app.domain.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Days since the Unix epoch (1970-01-01) in the device's local timezone.
 *
 * Using local-day index — not UTC-day index — keeps "today" stable across
 * daylight saving transitions and matches the user's mental model.
 *
 * INFERRED — NOT VERIFIED.
 */
object EpochDay {

    fun fromEpochMs(epochMs: Long, tz: TimeZone = TimeZone.getDefault()): Int {
        val dayStartMs = DaySlice.startOfDay(epochMs, tz)
        val c = Calendar.getInstance(tz).apply {
            clear()
            set(Calendar.YEAR, 1970)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val epochStartMs = c.timeInMillis
        return ((dayStartMs - epochStartMs) / (24L * 60L * 60L * 1000L)).toInt()
    }

    fun toEpochMs(epochDay: Int, tz: TimeZone = TimeZone.getDefault()): Long =
        DaySlice.startOfEpochDay(epochDay, tz)

    fun today(tz: TimeZone = TimeZone.getDefault()): Int = fromEpochMs(System.currentTimeMillis(), tz)

    fun plusDays(epochDay: Int, days: Int): Int = epochDay + days
}
