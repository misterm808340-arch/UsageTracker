package com.usagetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * An actively blocked app. Row exists while the block is active; deleted on
 * unblock or auto-expiry.
 * INFERRED — NOT VERIFIED.
 */
@Entity(
    tableName = "blocked_apps",
    indices = [
        Index("packageName", unique = true),
        Index("expiresAtMs")
    ],
    primaryKeys = ["packageName"]
)
data class BlockedAppEntity(
    val packageName: String,
    val label: String,
    val startedAtMs: Long,
    val expiresAtMs: Long,
    val focusMode: Boolean,
)
