package com.usagetracker.app.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.usagetracker.app.data.repository.SettingsRepositoryImpl
import com.usagetracker.app.data.repository.UsageRepositoryImpl
import com.usagetracker.app.domain.model.AppRanking
import com.usagetracker.app.domain.model.DaySlice
import com.usagetracker.app.domain.model.EpochDay
import com.usagetracker.app.domain.model.HourlyUsage
import com.usagetracker.app.domain.model.TimelineEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OverviewRange { HOURLY, SEVEN_DAYS, THIRTY_DAYS }

data class OverviewUiState(
    val loading: Boolean = false,
    val hasUsageAccess: Boolean = true,
    val todaysTotalMs: Long = 0L,
    val sessionCount: Int = 0,
    val range: OverviewRange = OverviewRange.HOURLY,
    val hourly: List<HourlyUsage> = emptyList(),
    val sevenDayTotals: List<Long> = emptyList(),
    val thirtyDayTotals: List<Long> = emptyList(),
    val mostUsed: List<com.usagetracker.app.domain.model.AppUsageInfo> = emptyList(),
    val mostOpened: List<com.usagetracker.app.domain.model.AppUsageInfo> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
    val lastUpdatedMs: Long = System.currentTimeMillis(),
)

/**
 * v1.2.2-fix:
 *  - ONE sync for today (previously the same range was synced twice).
 *  - 7/30-day bar totals now come from persisted per-day aggregates (Room),
 *    with correct per-day math (previously each app's whole-range total was
 *    dumped into whichever day it was last used — the bars were nonsense).
 *  - All heavy work on Dispatchers.IO.
 *  - Timeline is newest-first, system-app filtered (done in the repository).
 */
class OverviewViewModel(
    app: Application,
    private val usageRepo: UsageRepositoryImpl,
    private val settingsRepo: SettingsRepositoryImpl,
) : AndroidViewModel(app) {

    private val _loading = MutableStateFlow(false)
    private val _range = MutableStateFlow(OverviewRange.HOURLY)
    private val _state = MutableStateFlow(OverviewUiState())
    val state: StateFlow<OverviewUiState> = _state.asStateFlow()

    private val includeSystem: StateFlow<Boolean> =
        settingsRepo.showSystemApps.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init { refresh() }

    /** Charts already hold 7/30-day data — switching chips no longer re-syncs. */
    fun setRange(r: OverviewRange) {
        _range.value = r
        _state.value = _state.value.copy(range = r)
    }

    fun refresh() {
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, hasUsageAccess = settingsRepo.hasUsageAccess())
            if (!settingsRepo.hasUsageAccess()) {
                _state.value = _state.value.copy(loading = false)
                _loading.value = false
                return@launch
            }
            try {
                val now = withContext(Dispatchers.Default) { System.currentTimeMillis() }
                val incl = includeSystem.value

                // 1. Installed apps (throttled inside the repo) + ONE today sync.
                usageRepo.refreshInstalledApps(incl)
                val (ranking, hourly) = usageRepo.todayOverview(incl)

                // 2. 7/30-day per-day totals from persisted aggregates (no event query).
                val tz = java.util.TimeZone.getDefault()
                val today = EpochDay.today(tz)
                val seven = usageRepo.dailyTotalsForRange(today - 6, today)
                val thirty = usageRepo.dailyTotalsForRange(today - 29, today)
                val sevenDayTotals = (6 downTo 0).map { ago ->
                    val d = today - ago
                    seven.filter { it.epochDay == d }.sumOf { it.totalDurationMs }
                }
                val thirtyDayTotals = (29 downTo 0).map { ago ->
                    val d = today - ago
                    thirty.filter { it.epochDay == d }.sumOf { it.totalDurationMs }
                }

                // 3. Timeline — newest first, system apps filtered.
                val timeline = usageRepo.timelineForDay(today, incl)

                _state.value = OverviewUiState(
                    loading = false,
                    hasUsageAccess = true,
                    todaysTotalMs = ranking.totalDurationMs,
                    sessionCount = ranking.items.sumOf { it.sessionCount },
                    range = _range.value,
                    hourly = hourly,
                    sevenDayTotals = sevenDayTotals,
                    thirtyDayTotals = thirtyDayTotals,
                    mostUsed = ranking.items.take(5),
                    mostOpened = ranking.items.sortedByDescending { it.openCount }.take(5),
                    timeline = timeline,
                    lastUpdatedMs = now,
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false)
            } finally {
                _loading.value = false
            }
        }
    }
}
