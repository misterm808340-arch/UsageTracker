package com.usagetracker.app.ui.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.usagetracker.app.data.repository.SettingsRepositoryImpl
import com.usagetracker.app.data.repository.UsageRepositoryImpl
import com.usagetracker.app.domain.model.AppUsageInfo
import com.usagetracker.app.domain.model.DailyUsage
import com.usagetracker.app.domain.model.EpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppDetailUiState(
    val loading: Boolean = false,
    val packageName: String = "",
    val label: String = "",
    val todayMs: Long = 0L,
    val openCount: Int = 0,
    val sessionCount: Int = 0,
    val thirtyDayHistory: List<DailyUsage> = emptyList(),
    val installedLabel: String = "",
)

class AppDetailViewModel(
    app: Application,
    private val usageRepo: UsageRepositoryImpl,
    private val settingsRepo: SettingsRepositoryImpl,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(AppDetailUiState())
    val state: StateFlow<AppDetailUiState> = _state.asStateFlow()

    fun load(packageName: String, label: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, packageName = packageName, label = label)
            val tz = java.util.TimeZone.getDefault()
            val today = EpochDay.today(tz)
            // Make sure today's sessions exist before reading history (cheap, one day).
            usageRepo.syncRange(EpochDay.toEpochMs(today, tz), System.currentTimeMillis())
            val last30 = EpochDay.plusDays(today, -29) .. today
            val history = usageRepo.perAppHistory(packageName, last30.first, last30.last)
            val todayUsage = history.lastOrNull { it.epochDay == today }
            _state.value = AppDetailUiState(
                loading = false,
                packageName = packageName,
                label = label,
                todayMs = todayUsage?.totalDurationMs ?: 0L,
                openCount = history.sumOf { it.openCount },
                sessionCount = history.sumOf { it.sessionCount },
                thirtyDayHistory = history,
                installedLabel = label,
            )
        }
    }
}
