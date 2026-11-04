package com.usagetracker.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.usagetracker.app.ads.InterstitialAdManager
import com.usagetracker.app.di.AppViewModelFactory
import com.usagetracker.app.ui.apps.AppDetailScreen
import com.usagetracker.app.ui.apps.AppsScreen
import com.usagetracker.app.ui.blocker.BlockerScreen
import com.usagetracker.app.ui.history.HistoryScreen
import com.usagetracker.app.ui.onboarding.OnboardingScreen
import com.usagetracker.app.ui.overview.OverviewScreen
import com.usagetracker.app.ui.settings.SettingsScreen
import androidx.activity.ComponentActivity

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    object Overview  : Dest("overview",  "Overview",  Icons.Outlined.Dashboard)
    object Apps      : Dest("apps",      "Apps",      Icons.Outlined.Apps)
    object Blocker   : Dest("blocker",   "Blocker",   Icons.Outlined.Block)
    object History   : Dest("history",   "History",   Icons.Outlined.CalendarMonth)
    object Settings  : Dest("settings",  "Settings",  Icons.Outlined.Settings)
}

private val bottomNav = listOf(Dest.Overview, Dest.Apps, Dest.Blocker, Dest.History, Dest.Settings)

object Routes {
    const val ONBOARDING = "onboarding"
    const val OVERVIEW   = "overview"
    const val APPS       = "apps"
    const val APP_DETAIL = "app_detail/{packageName}/{label}"
    const val BLOCKER    = "blocker"
    const val HISTORY    = "history"
    const val SETTINGS   = "settings"
    fun appDetail(pkg: String, label: String): String =
        "app_detail/$pkg/${java.net.URLEncoder.encode(label, "UTF-8")}"
}

@Composable
fun UsageTrackerNavHost(factory: AppViewModelFactory) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val showBottom = current in bottomNav.map { it.route }

    // Helper: show an interstitial (if available and enough time passed) before
    // running the given navigation action. The action runs regardless — the ad
    // never blocks navigation.
    val openAppWithInterstitial: (String, String) -> Unit = { pkg, label ->
        (ctx as? ComponentActivity)?.let { InterstitialAdManager.show(it) }
        nav.navigate(Routes.appDetail(pkg, label))
    }

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    bottomNav.forEach { d ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == d.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(d.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(d.icon, contentDescription = d.label) },
                            label = { Text(d.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.ONBOARDING,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.ONBOARDING) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val vm = ViewModelProvider(it, factory)[com.usagetracker.app.ui.onboarding.OnboardingViewModel::class.java]
                val s by vm.state.collectAsStateWithLifecycle()
                OnboardingScreen(
                    state = s,
                    onGrantUsageAccess = {
                        val i = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(i)
                    },
                    onSkip = {
                        vm.complete()
                        nav.navigate(Routes.OVERVIEW) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    },
                    onGetStarted = {
                        vm.complete()
                        nav.navigate(Routes.OVERVIEW) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    },
                    onRefresh = { vm.refreshAccess() },
                )
            }
            composable(Routes.OVERVIEW) {
                val vm = ViewModelProvider(it, factory)[com.usagetracker.app.ui.overview.OverviewViewModel::class.java]
                val s by vm.state.collectAsStateWithLifecycle()
                OverviewScreen(
                    state = s,
                    onSetRange = vm::setRange,
                    onRefresh = vm::refresh,
                    onOpenApp = openAppWithInterstitial,
                )
            }
            composable(Routes.APPS) {
                val vm = ViewModelProvider(it, factory)[com.usagetracker.app.ui.apps.AppsViewModel::class.java]
                val s by vm.state.collectAsStateWithLifecycle()
                AppsScreen(
                    state = s,
                    onQuery = vm::setQuery,
                    onPeriod = vm::setPeriod,
                    onSort = vm::setSortMode,
                    onRefresh = vm::refresh,
                    onAppClick = openAppWithInterstitial,
                )
            }
            composable(
                route = Routes.APP_DETAIL,
                arguments = listOf(
                    navArgument("packageName") { type = NavType.StringType },
                    navArgument("label") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val pkg = backStackEntry.arguments?.getString("packageName") ?: return@composable
                val label = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("label") ?: "", "UTF-8")
                val vm = ViewModelProvider(backStackEntry, factory)[com.usagetracker.app.ui.apps.AppDetailViewModel::class.java]
                val s by vm.state.collectAsStateWithLifecycle()
                AppDetailScreen(
                    state = s,
                    onLoad = { vm.load(pkg, label) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.BLOCKER) {
                val vm = ViewModelProvider(it, factory)[com.usagetracker.app.ui.blocker.BlockerViewModel::class.java]
                val s by vm.state.collectAsStateWithLifecycle()
                BlockerScreen(
                    state = s,
                    onBlockApp = { p, l, m -> vm.blockApp(p, l, m) },
                    onUnblock = vm::unblock,
                    onSetBlockNotifications = vm::setBlockNotifications,
                    onSetLimit = { p, l, n, e -> vm.setLimit(p, l, n, e) },
                    onRemoveLimit = vm::removeLimit,
                    onListInstalled = { vm.listInstalledForPicker(false) },
                )
            }
            composable(Routes.HISTORY) {
                val vm = ViewModelProvider(it, factory)[com.usagetracker.app.ui.history.HistoryViewModel::class.java]
                val s by vm.state.collectAsStateWithLifecycle()
                HistoryScreen(
                    state = s,
                    onPrevDay = vm::previousDay,
                    onNextDay = vm::nextDay,
                    onPickDay = vm::loadDay,
                    onOpenApp = openAppWithInterstitial,
                )
            }
            composable(Routes.SETTINGS) {
                val vm = ViewModelProvider(it, factory)[com.usagetracker.app.ui.settings.SettingsViewModel::class.java]
                val s by vm.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = s,
                    onSetTheme = vm::setTheme,
                    onSetShowSystemApps = vm::setShowSystemApps,
                    onRefreshUsage = vm::refreshUsageData,
                    onClearCache = vm::clearCachedData,
                    onDeleteAll = vm::deleteAllData,
                    onExportCsv = vm::exportCsv,
                    onToastShown = vm::toastShown,
                )
            }
        }
    }
}
