package com.usagetracker.app.ui.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.usagetracker.app.data.repository.SettingsRepositoryImpl
import com.usagetracker.app.data.repository.UsageRepositoryImpl
import com.usagetracker.app.domain.model.AppUsageInfo
import com.usagetracker.app.domain.util.AppSortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class AppsPeriod { TODAY, YESTERDAY, SEVEN_DAYS, THIRTY_DAYS }

data class AppsUiState(
    val loading: Boolean = false,
    val hasUsageAccess: Boolean = true,
    val query: String = "",
    val period: AppsPeriod = AppsPeriod.TODAY,
    val sortMode: AppSortMode = AppSortMode.MOST_USED,
    val items: List<AppUsageInfo> = emptyList(),
)

/**
 * v1.2.2-fix: search and sort now operate on the in-memory ranking instead of
 * re-running a full usage sync on every keystroke. Only a period change (or
 * explicit refresh) triggers a new range query, which runs on Dispatchers.IO.
 */
class AppsViewModel(
    app: Application,
    private val usageRepo: UsageRepositoryImpl,
    private val settingsRepo: SettingsRepositoryImpl,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    /** Last full ranking for the selected period — search/sort reuse this. */
    private var lastRanking: List<AppUsageInfo> = emptyList()

    private val includeSystem: StateFlow<Boolean> =
        settingsRepo.showSystemApps.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init { reload() }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        applyLocalFilters()
    }

    fun setSortMode(m: AppSortMode) {
        _state.value = _state.value.copy(sortMode = m)
        applyLocalFilters()
    }

    fun setPeriod(p: AppsPeriod) {
        if (_state.value.period == p) return
        _state.value = _state.value.copy(period = p)
        reload()
    }

    fun refresh() = reload()

    private fun applyLocalFilters() {
        val q = _state.value.query.trim().lowercase()
        val filtered = if (q.isEmpty()) lastRanking
            else lastRanking.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        _state.value = _state.value.copy(
            items = com.usagetracker.app.domain.util.AppListSorter.sort(filtered, _state.value.sortMode),
        )
    }

    private fun reload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, hasUsageAccess = settingsRepo.hasUsageAccess())
            if (!settingsRepo.hasUsageAccess()) {
                lastRanking = emptyList()
                _state.value = _state.value.copy(loading = false, items = emptyList())
                return@launch
            }
            try {
                val incl = includeSystem.value
                usageRepo.refreshInstalledApps(incl)
                val now = System.currentTimeMillis()
                val start: Long
                val end: Long = now
                when (_state.value.period) {
                    AppsPeriod.TODAY      -> start = com.usagetracker.app.domain.model.DaySlice.startOfDay(now)
                    AppsPeriod.YESTERDAY  -> {
                        val yStart = com.usagetracker.app.domain.model.DaySlice.startOfDay(now - TimeUnit.DAYS.toMillis(1))
                        start = yStart
                    }
                    AppsPeriod.SEVEN_DAYS -> start = now - TimeUnit.DAYS.toMillis(7)
                    AppsPeriod.THIRTY_DAYS -> start = now - TimeUnit.DAYS.toMillis(30)
                }
                val ranking = usageRepo.rangeRanking(start, end, incl)
                lastRanking = ranking.items
                applyLocalFilters()
                _state.value = _state.value.copy(loading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}
