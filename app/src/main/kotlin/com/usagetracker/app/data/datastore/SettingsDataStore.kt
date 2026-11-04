package com.usagetracker.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * DataStore-backed user preferences.
 * INFERRED — NOT VERIFIED.
 */
class SettingsDataStore(private val context: Context) {

    object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val THEME = stringPreferencesKey("theme")
        val SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        val BLOCK_NOTIFICATIONS = booleanPreferencesKey("block_notifications")
        val LAST_SYNC_MS = intPreferencesKey("last_sync_ms")
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }
    suspend fun setOnboarded(v: Boolean) { context.dataStore.edit { it[Keys.ONBOARDED] = v } }

    val theme: Flow<ThemeMode> = context.dataStore.data.map {
        when (it[Keys.THEME] ?: "system") {
            "light" -> ThemeMode.LIGHT
            "dark"  -> ThemeMode.DARK
            else    -> ThemeMode.SYSTEM
        }
    }
    suspend fun setTheme(v: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = when (v) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK  -> "dark"
            ThemeMode.SYSTEM -> "system"
        } }
    }

    val showSystemApps: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SHOW_SYSTEM_APPS] ?: false }
    suspend fun setShowSystemApps(v: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SYSTEM_APPS] = v }
    }

    val blockNotifications: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.BLOCK_NOTIFICATIONS] ?: false }
    suspend fun setBlockNotifications(v: Boolean) {
        context.dataStore.edit { it[Keys.BLOCK_NOTIFICATIONS] = v }
    }

    val lastSyncMs: Flow<Long> =
        context.dataStore.data.map { (it[Keys.LAST_SYNC_MS] ?: 0).toLong() }
    suspend fun setLastSyncMs(v: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_MS] = (v and 0xFFFFFFFFL).toInt() }
    }
}
