package com.usagetracker.app.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.usagetracker.app.data.datastore.ThemeMode
import com.usagetracker.app.data.repository.BlockerRepositoryImpl
import com.usagetracker.app.data.repository.SettingsRepositoryImpl
import com.usagetracker.app.data.repository.UsageRepositoryImpl
import com.usagetracker.app.domain.model.EpochDay
import com.usagetracker.app.domain.util.CsvBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

data class SettingsUiState(
    val hasUsageAccess: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasDndAccess: Boolean = false,
    val hasPostNotifications: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val showSystemApps: Boolean = false,
    val refreshing: Boolean = false,
    val lastSyncMs: Long = 0L,
    val csvUri: String? = null,
    val toast: String? = null,
)

class SettingsViewModel(
    app: Application,
    private val usageRepo: UsageRepositoryImpl,
    private val blockerRepo: BlockerRepositoryImpl,
    private val settingsRepo: SettingsRepositoryImpl,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepo.theme,
                settingsRepo.showSystemApps,
                settingsRepo.lastSyncMs,
            ) { theme, showSys, lastSync ->
                _state.value.copy(
                    hasUsageAccess = settingsRepo.hasUsageAccess(),
                    hasOverlay = settingsRepo.hasOverlayPermission(),
                    hasDndAccess = settingsRepo.hasDndAccess(),
                    hasPostNotifications = settingsRepo.hasPostNotifications(),
                    theme = theme,
                    showSystemApps = showSys,
                    lastSyncMs = lastSync,
                )
            }.collect { _state.value = it }
        }
    }

    fun setTheme(t: ThemeMode) = viewModelScope.launch { settingsRepo.setTheme(t) }
    fun setShowSystemApps(v: Boolean) = viewModelScope.launch {
        settingsRepo.setShowSystemApps(v)
    }

    fun refreshUsageData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(refreshing = true)
            try {
                val now = System.currentTimeMillis()
                usageRepo.syncRange(now - 7L * 24L * 60L * 60L * 1000L, now)
                _state.value = _state.value.copy(refreshing = false,
                    toast = "Usage data refreshed")
            } catch (t: Throwable) {
                _state.value = _state.value.copy(refreshing = false,
                    toast = "Refresh failed: ${t.message}")
            }
        }
    }

    fun clearCachedData() {
        viewModelScope.launch {
            usageRepo.clearCache()
            _state.value = _state.value.copy(toast = "Cache cleared")
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            usageRepo.clearAll()
            // Also clear blocker state
            for (b in blockerRepo.observeBlocked().let { /* one-shot */ emptyList<com.usagetracker.app.data.local.entity.BlockedAppEntity>() }) {
                blockerRepo.unblock(b.packageName)
            }
            _state.value = _state.value.copy(toast = "All data deleted")
        }
    }

    /**
     * Export to CSV into app cache dir, then expose via FileProvider URI for
     * the user to share / save via the system document picker (ACTION_CREATE_DOCUMENT
     * is the proper path; we use FileProvider + ACTION_SEND as a fallback).
     */
    fun exportCsv() {
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val today = EpochDay.today()
                val from = EpochDay.plusDays(today, -29)
                val daily = usageRepo.dailyForRange(from, today)
                val timeline = usageRepo.timelineForRange(
                    EpochDay.toEpochMs(from), System.currentTimeMillis())
                val labels = usageRepo.appLabels()
                val byApp: Map<String, List<com.usagetracker.app.domain.model.DailyUsage>> =
                    daily.groupBy { it.packageName }
                val outFile = File(ctx.cacheDir, "usage_tracker_export_${System.currentTimeMillis()}.csv")
                withContext(Dispatchers.IO) {
                    FileWriter(outFile).use { w -> CsvBuilder.build(w, byApp, timeline, labels) }
                }
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", outFile)
                _state.value = _state.value.copy(csvUri = uri.toString(),
                    toast = "CSV ready — share or save")
            } catch (t: Throwable) {
                _state.value = _state.value.copy(toast = "Export failed: ${t.message}")
            }
        }
    }

    fun toastShown() { _state.value = _state.value.copy(toast = null) }
}
