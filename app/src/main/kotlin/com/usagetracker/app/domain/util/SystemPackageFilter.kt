package com.usagetracker.app.domain.util

/**
 * Filters out Android system packages that are not user-launchable.
 * INFERRED — NOT VERIFIED.
 */
object SystemPackageFilter {

    private val KEEP_PREFIXES = setOf(
        "com.android.chrome",   // user-installed even though com.android
    )

    private val DROP_PREFIXES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.contacts",
        "com.android.calendar",
        "com.android.camera",
        "com.android.dialer",
        "com.android.mms",
        "com.android.messaging",
        "com.android.email",
        "com.android.browser",
        "com.android.nfc",
        "com.android.providers",
        "com.google.android.providers",
        "android",
        "com.android.inputmethod",
        "com.android.shell",
    )

    private val DROP_EXACT = setOf(
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.google.android.packageinstaller",
    )

    fun isUserFacing(packageName: String, isSystem: Boolean, includeSystem: Boolean): Boolean {
        if (DROP_EXACT.contains(packageName)) return false
        if (DROP_PREFIXES.any { packageName.startsWith(it) }) return false
        if (KEEP_PREFIXES.any { packageName.startsWith(it) }) return true
        if (isSystem && !includeSystem) return false
        return true
    }
}
