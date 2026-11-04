package com.usagetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Daily open-count limit per app. When the open-count for [epochDay] reaches
 * [limit], the BlockerService starts covering the app.
 * INFERRED — NOT VERIFIED.
 */
@Entity(
    tableName = "launch_limits",
    indices = [Index("packageName", unique = true)],
    primaryKeys = ["packageName"]
)
data class LaunchLimitEntity(
    val packageName: String,
    val label: String,
    val limit: Int,
    val enabled: Boolean,
    val updatedAtMs: Long,
)
