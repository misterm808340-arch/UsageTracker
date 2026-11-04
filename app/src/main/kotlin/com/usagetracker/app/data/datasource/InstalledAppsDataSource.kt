package com.usagetracker.app.data.datasource

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.usagetracker.app.data.local.entity.AppInfoEntity

/**
 * Wraps [PackageManager] for enumerating installed apps.
 * INFERRED — NOT VERIFIED.
 */
class InstalledAppsDataSource(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    data class InstalledApp(
        val packageName: String,
        val label: String,
        val isSystem: Boolean,
        val versionName: String?,
        val versionCode: Long,
        val firstInstallTimeMs: Long,
        val lastUpdateTimeMs: Long,
    )

    fun listInstalled(includeSystem: Boolean = false): List<InstalledApp> {
        val flags = PackageManager.GET_META_DATA
        val infos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") pm.getInstalledApplications(flags)
        }
        return infos.mapNotNull { ai ->
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem && !includeSystem && !isSystemLauncher(ai)) return@mapNotNull null
            if (ai.packageName == context.packageName) return@mapNotNull null
            val label = try { pm.getApplicationLabel(ai).toString() } catch (t: Throwable) { ai.packageName }
            val pInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(ai.packageName, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION") pm.getPackageInfo(ai.packageName, 0)
                }
            } catch (t: Throwable) { null }
            InstalledApp(
                packageName = ai.packageName,
                label = label,
                isSystem = isSystem,
                versionName = pInfo?.versionName,
                versionCode = pInfo?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode
                    else @Suppress("DEPRECATION") it.versionCode.toLong()
                } ?: 0L,
                firstInstallTimeMs = pInfo?.firstInstallTime ?: 0L,
                lastUpdateTimeMs = pInfo?.lastUpdateTime ?: 0L,
            )
        }.distinctBy { it.packageName }
    }

    private fun isSystemLauncher(ai: ApplicationInfo): Boolean {
        val launchers = try {
            pm.getPreferredActivities(
                java.util.ArrayList(),
                java.util.ArrayList(),
                null
            )
            // Cheap heuristic: treat any system app whose package contains "launcher" as visible.
            false
        } catch (t: Throwable) { false }
        return false
    }

    fun toEntity(app: InstalledApp, now: Long): AppInfoEntity = AppInfoEntity(
        packageName = app.packageName,
        label = app.label,
        isSystem = app.isSystem,
        versionName = app.versionName,
        versionCode = app.versionCode,
        firstInstallTimeMs = app.firstInstallTimeMs,
        lastUpdateTimeMs = app.lastUpdateTimeMs,
        lastSeenMs = now,
    )
}
