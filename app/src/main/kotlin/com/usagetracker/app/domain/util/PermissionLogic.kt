package com.usagetracker.app.domain.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Centralized permission checks and intent launches.
 *
 * v1.3.0-fix: `notificationPermissionIntent(context)` now takes a Context and
 * uses `context.packageName` instead of the hardcoded `"android"` literal,
 * which was sending the user to the Android System notification settings
 * instead of this app's. We also use `NotificationManagerCompat.areNotificationsEnabled()`
 * as an extra check (pre-Android-13 users can disable notifications at the app
 * level via system Settings even without the runtime POST_NOTIFICATIONS perm).
 */
object PermissionLogic {

    fun canDrawOverlays(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context)
        else true

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun hasNotificationPolicyAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return try { nm.isNotificationPolicyAccessGranted } catch (t: Throwable) { false }
    }

    /**
     * Combined check: on Android 13+ returns true only when the runtime
     * POST_NOTIFICATIONS permission is granted; on all versions returns true
     * only when the app's notifications are enabled at the system level (via
     * [NotificationManagerCompat.areNotificationsEnabled()]). This catches the
     * pre-Android-13 case where the user disabled notifications for the app in
     * system Settings without going through a runtime permission dialog.
     */
    fun hasPostNotifications(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        if (!runtimeGranted) return false
        return androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

    fun usageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun notificationPolicyAccessIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    /**
     * v1.3.0-fix: opens THIS APP's notification settings (was opening the
     * "android" system package's settings, which the user couldn't act on).
     * Used by the Blocker screen as a fallback when POST_NOTIFICATIONS is
     * permanently denied — sending the user to the system permission dialog
     * would no-op, so we send them to the per-app settings where they can
     * re-enable notifications.
     */
    fun notificationPermissionIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        else
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra("android.provider.extra.APP_PACKAGE", context.packageName)

    fun appDetailsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
}
