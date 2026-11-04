package com.usagetracker.app.data.repository

import android.content.Context
import com.usagetracker.app.data.datastore.SettingsDataStore
import com.usagetracker.app.data.datastore.ThemeMode
import com.usagetracker.app.data.datasource.UsageStatsDataSource
import com.usagetracker.app.domain.util.PermissionLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Settings: theme, system-apps, sync, usage-access status, CSV.
 * INFERRED — NOT VERIFIED.
 */
class SettingsRepositoryImpl(
    private val context: Context,
    private val store: SettingsDataStore,
    private val usageStats: UsageStatsDataSource,
) {

    val onboarded: Flow<Boolean> = store.onboarded
    suspend fun setOnboarded(v: Boolean) = store.setOnboarded(v)

    val theme: Flow<ThemeMode> = store.theme
    suspend fun setTheme(v: ThemeMode) = store.setTheme(v)

    val showSystemApps: Flow<Boolean> = store.showSystemApps
    suspend fun setShowSystemApps(v: Boolean) = store.setShowSystemApps(v)

    val blockNotifications: Flow<Boolean> = store.blockNotifications
    suspend fun setBlockNotifications(v: Boolean) = store.setBlockNotifications(v)

    val lastSyncMs: Flow<Long> = store.lastSyncMs

    fun hasUsageAccess(): Boolean = PermissionLogic.hasUsageAccess(context)
    fun hasOverlayPermission(): Boolean = PermissionLogic.canDrawOverlays(context)
    fun hasDndAccess(): Boolean = PermissionLogic.hasNotificationPolicyAccess(context)
    fun hasPostNotifications(): Boolean = PermissionLogic.hasPostNotifications(context)
}
