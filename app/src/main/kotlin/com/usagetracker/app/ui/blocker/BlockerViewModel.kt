package com.usagetracker.app.ui.blocker

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.usagetracker.app.data.local.entity.AppInfoEntity
import com.usagetracker.app.data.local.entity.BlockedAppEntity
import com.usagetracker.app.data.local.entity.LaunchLimitEntity
import com.usagetracker.app.data.repository.BlockerRepositoryImpl
import com.usagetracker.app.data.repository.SettingsRepositoryImpl
import com.usagetracker.app.data.repository.UsageRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class BlockerUiState(
    val loading: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val hasDndAccess: Boolean = false,
    val hasPostNotifications: Boolean = false,
    val blockNotifications: Boolean = false,
    val blocked: List<BlockedAppEntity> = emptyList(),
    val limits: List<LaunchLimitEntity> = emptyList(),
)

class BlockerViewModel(
    app: Application,
    private val blockerRepo: BlockerRepositoryImpl,
    private val settingsRepo: SettingsRepositoryImpl,
    private val usageRepo: UsageRepositoryImpl,
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(BlockerUiState())
    val state: StateFlow<BlockerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                blockerRepo.observeBlocked(),
                blockerRepo.observeLimits(),
                settingsRepo.blockNotifications,
            ) { blocked, limits, blockNotif ->
                BlockerUiState(
                    loading = false,
                    hasOverlay = settingsRepo.hasOverlayPermission(),
                    hasUsageAccess = settingsRepo.hasUsageAccess(),
                    hasDndAccess = settingsRepo.hasDndAccess(),
                    hasPostNotifications = settingsRepo.hasPostNotifications(),
                    blockNotifications = blockNotif,
                    blocked = blocked,
                    limits = limits,
                )
            }.collect { _state.value = it }
        }
    }

    fun blockApp(pkg: String, label: String, minutes: Int) {
        viewModelScope.launch {
            blockerRepo.blockApp(pkg, label, TimeUnit.MINUTES.toMillis(minutes.toLong()), focusMode = true)
            startBlockerService()
        }
    }
    fun unblock(pkg: String) {
        viewModelScope.launch {
            blockerRepo.unblock(pkg)
        }
    }
    fun setBlockNotifications(v: Boolean) {
        viewModelScope.launch { settingsRepo.setBlockNotifications(v) }
    }
    fun setLimit(pkg: String, label: String, limit: Int, enabled: Boolean) {
        viewModelScope.launch { blockerRepo.setLimit(pkg, label, limit, enabled) }
    }
    fun removeLimit(pkg: String) {
        viewModelScope.launch { blockerRepo.removeLimit(pkg) }
    }

    suspend fun listInstalledForPicker(includeSystem: Boolean): List<AppInfoEntity> {
        usageRepo.refreshInstalledApps(includeSystem)
        // Direct DAO one-shot:
        return com.usagetracker.app.data.local.UsageDatabase.get(getApplication()).appInfoDao().getAll()
    }

    private fun startBlockerService() {
        val ctx = getApplication<Application>()
        val svc = Intent(ctx, com.usagetracker.app.service.BlockerService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ctx.startForegroundService(svc)
        } else {
            ctx.startService(svc)
        }
    }
}
