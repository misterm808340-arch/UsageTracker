package com.usagetracker.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.usagetracker.app.data.repository.SettingsRepositoryImpl
import com.usagetracker.app.data.repository.UsageRepositoryImpl
import com.usagetracker.app.domain.model.AppUsageInfo
import com.usagetracker.app.domain.model.EpochDay
import com.usagetracker.app.domain.model.TimelineEntry
import com.usagetracker.app.domain.util.SystemPackageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HistoryUiState(
    val loading: Boolean = false,
    val hasUsageAccess: Boolean = true,
    val epochDay: Int = EpochDay.today(),
    val totalMs: Long = 0L,
    val sessionCount: Int = 0,
    val ranked: List<AppUsageInfo> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
)

/**
 * v1.2.2-fix: the viewed day is synced live (cheap, one day) so history is
 * fresh even after navigating backwards; the system-app filter now respects
 * the showSystemApps setting (the old `incl` variable read nothing); heavy
 * work runs on Dispatchers.IO; timeline is newest-first and system-filtered.
 */
class HistoryViewModel(
    app: Application,
    private val usageRepo: UsageRepositoryImpl,
    private val settingsRepo: SettingsRepositoryImpl,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init { loadDay(EpochDay.today()) }

    fun previousDay() = loadDay(_state.value.epochDay - 1)
    fun nextDay() = loadDay(_state.value.epochDay + 1)

    fun loadDay(day: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, epochDay = day,
                hasUsageAccess = settingsRepo.hasUsageAccess())
            if (!settingsRepo.hasUsageAccess()) {
                _state.value = _state.value.copy(loading = false)
                return@launch
            }
            try {
                withContext(Dispatchers.IO) {
                    val tz = java.util.TimeZone.getDefault()
                    val dayStart = EpochDay.toEpochMs(day, tz)
                    val dayEnd = dayStart + 24L * 60L * 60L * 1000L - 1L
                    val now = System.currentTimeMillis()
                    if (dayEnd >= now - 60_000L) {
                        // Today (or future) — live sync so the data is current.
                        usageRepo.syncRange(dayStart, now)
                    }
                    val includeSystem = try {
                        settingsRepo.showSystemApps.first()
                    } catch (t: Throwable) { false }
                    val timeline = usageRepo.timelineForDay(day, includeSystem)

                    val sessions = com.usagetracker.app.data.local.UsageDatabase.get(getApplication())
                        .usageSessionDao().forDay(day)
                    val labels = usageRepo.appLabels()
                    val sys = usageRepo.appSystemFlags()
                    val byPkg = sessions.groupBy { it.packageName }
                    val items = byPkg.entries.map { (pkg, list) ->
                        AppUsageInfo(
                            packageName = pkg,
                            label = labels[pkg] ?: pkg,
                            totalDurationMs = list.sumOf { it.durationMs },
                            sessionCount = list.size,
                            openCount = list.size,
                            firstUsedMs = list.minOf { it.startTimeMs },
                            lastUsedMs = list.maxOf { it.endTimeMs },
                            isSystem = sys[pkg] ?: false,
                        )
                    }.filter {
                        SystemPackageFilter.isUserFacing(it.packageName, it.isSystem, includeSystem)
                    }.sortedByDescending { it.totalDurationMs }

                    _state.value = HistoryUiState(
                        loading = false,
                        hasUsageAccess = true,
                        epochDay = day,
                        totalMs = items.sumOf { it.totalDurationMs },
                        sessionCount = items.sumOf { it.sessionCount },
                        ranked = items,
                        timeline = timeline,
                    )
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}
