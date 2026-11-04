package com.usagetracker.app.domain.model

/**
 * A single normalized foreground/background transition parsed from
 * [android.app.usage.UsageEvents]. The original app likely had an equivalent
 * struct used by SessionDetector.
 *
 * INFERRED — NOT VERIFIED.
 */
data class UsageEventRecord(
    val packageName: String,
    val timestampMs: Long,
    val eventType: Type,
) {
    enum class Type {
        ACTIVITY_RESUMED,   // MOVE_TO_FOREGROUND
        ACTIVITY_PAUSED,    // MOVE_TO_BACKGROUND
        USER_INTERACTION,
        SCREEN_NON_INTERACTIVE,
        SCREEN_INTERACTIVE,
        OTHER;

        companion object {
            fun fromUsageEventsEventType(code: Int): Type = when (code) {
                // UsageEvents constants — values stable since API 21+
                1 -> ACTIVITY_RESUMED
                2 -> ACTIVITY_PAUSED
                7 -> USER_INTERACTION
                9 -> SCREEN_NON_INTERACTIVE
                8 -> SCREEN_INTERACTIVE
                else -> OTHER
            }
        }
    }
}
