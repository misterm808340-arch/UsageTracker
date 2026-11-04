package com.usagetracker.app.domain.model

/**
 * A continuous foreground usage interval for a single app.
 *
 * INFERRED — NOT VERIFIED: the original class name and field set were not
 * recoverable from the contact sheet. This is a plausible reconstruction.
 */
data class UsageBucket(
    val packageName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
) {
    val durationMs: Long get() = (endTimeMs - startTimeMs).coerceAtLeast(0L)
}
