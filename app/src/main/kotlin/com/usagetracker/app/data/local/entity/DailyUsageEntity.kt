package com.usagetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Pre-aggregated daily totals per app. One row per (epochDay, packageName).
 * INFERRED — NOT VERIFIED.
 */
@Entity(
    tableName = "daily_usage",
    indices = [
        Index("epochDay"),
        Index("packageName"),
        Index(value = ["epochDay", "packageName"], unique = true)
    ],
    primaryKeys = ["epochDay", "packageName"]
)
data class DailyUsageEntity(
    val epochDay: Int,
    val packageName: String,
    val totalDurationMs: Long,
    val sessionCount: Int,
    val openCount: Int,
    val updatedAtMs: Long,
)
