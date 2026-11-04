package com.usagetracker.app.di

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.usagetracker.app.UsageTrackerApplication
import com.usagetracker.app.data.repository.BlockerRepositoryImpl
import com.usagetracker.app.data.repository.SettingsRepositoryImpl
import com.usagetracker.app.data.repository.UsageRepositoryImpl
import com.usagetracker.app.ui.apps.AppDetailViewModel
import com.usagetracker.app.ui.apps.AppsViewModel
import com.usagetracker.app.ui.blocker.BlockerViewModel
import com.usagetracker.app.ui.history.HistoryViewModel
import com.usagetracker.app.ui.onboarding.OnboardingViewModel
import com.usagetracker.app.ui.overview.OverviewViewModel
import com.usagetracker.app.ui.settings.SettingsViewModel

/**
 * Single factory that hands every screen its ViewModel with the right
 * repository deps. Activity-scoped, so all ViewModels share the same
 * underlying database.
 *
 * INFERRED — NOT VERIFIED.
 */
@Suppress("UNCHECKED_CAST")
class AppViewModelFactory(
    private val app: Application,
    private val usageRepo: UsageRepositoryImpl,
    private val blockerRepo: BlockerRepositoryImpl,
    private val settingsRepo: SettingsRepositoryImpl,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(OverviewViewModel::class.java) ->
            OverviewViewModel(app, usageRepo, settingsRepo) as T
        modelClass.isAssignableFrom(AppsViewModel::class.java) ->
            AppsViewModel(app, usageRepo, settingsRepo) as T
        modelClass.isAssignableFrom(AppDetailViewModel::class.java) ->
            AppDetailViewModel(app, usageRepo, settingsRepo) as T
        modelClass.isAssignableFrom(BlockerViewModel::class.java) ->
            BlockerViewModel(app, blockerRepo, settingsRepo, usageRepo) as T
        modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
            HistoryViewModel(app, usageRepo, settingsRepo) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(app, usageRepo, blockerRepo, settingsRepo) as T
        modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
            OnboardingViewModel(settingsRepo) as T
        else -> throw IllegalArgumentException("Unknown VM class: ${modelClass.name}")
    }
}
