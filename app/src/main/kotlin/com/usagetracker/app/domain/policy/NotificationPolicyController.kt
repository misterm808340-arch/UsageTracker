package com.usagetracker.app.domain.policy

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Controls Do-Not-Disturb policy. Used by the Blocker to silence
 * notifications during focus mode (where Android + permissions allow).
 *
 * INFERRED — NOT VERIFIED.
 */
class NotificationPolicyController(private val context: Context) {

    private val nm: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasAccess(): Boolean = try {
        nm.isNotificationPolicyAccessGranted
    } catch (t: Throwable) { false }

    fun requestAccessIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    /**
     * Enable interruption filter. Returns true on success.
     * Requires [NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED].
     */
    fun enableDnd(): Boolean {
        if (!hasAccess()) return false
        return try {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            true
        } catch (t: Throwable) { false }
    }

    fun restoreFilter(): Boolean {
        if (!hasAccess()) return false
        return try {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            true
        } catch (t: Throwable) { false }
    }
}
