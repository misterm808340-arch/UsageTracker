package com.usagetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached metadata for an installed app.
 * INFERRED — NOT VERIFIED.
 */
@Entity(
    tableName = "app_info",
    indices = [Index("packageName", unique = true)]
)
data class AppInfoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTimeMs: Long,
    val lastUpdateTimeMs: Long,
    val lastSeenMs: Long,
)
