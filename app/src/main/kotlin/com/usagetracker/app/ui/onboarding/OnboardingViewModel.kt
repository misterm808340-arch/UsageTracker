package com.usagetracker.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagetracker.app.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val hasUsageAccess: Boolean = false,
)

class OnboardingViewModel(
    private val settingsRepo: SettingsRepositoryImpl,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = OnboardingUiState(hasUsageAccess = settingsRepo.hasUsageAccess())
        }
    }

    fun refreshAccess() {
        _state.value = _state.value.copy(hasUsageAccess = settingsRepo.hasUsageAccess())
    }

    fun complete() {
        viewModelScope.launch { settingsRepo.setOnboarded(true) }
    }
}
