package com.usagetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * One foreground usage session — paired ACTIVITY_RESUMED → ACTIVITY_PAUSED.
 * INFERRED — NOT VERIFIED.
 */
@Entity(
    tableName = "usage_sessions",
    indices = [
        Index("packageName"),
        Index("startTimeMs"),
        Index("epochDay"),
        Index(value = ["packageName", "startTimeMs"], unique = true)
    ],
    primaryKeys = ["packageName", "startTimeMs"]
)
data class UsageSessionEntity(
    val packageName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val epochDay: Int,
    val hourOfDay: Int,
)
