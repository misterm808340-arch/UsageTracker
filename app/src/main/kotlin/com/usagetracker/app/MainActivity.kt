package com.usagetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.usagetracker.app.data.datastore.ThemeMode
import com.usagetracker.app.ui.navigation.UsageTrackerNavHost
import com.usagetracker.app.ui.theme.UsageTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.hashCode(), Color.Transparent.hashCode()),
            navigationBarStyle = SystemBarStyle.auto(Color.Transparent.hashCode(), Color.Transparent.hashCode()),
        )
        val app = application as UsageTrackerApplication
        setContent {
            val settingsVm: com.usagetracker.app.ui.settings.SettingsViewModel =
                viewModel(factory = app.viewModelFactory)
            val themeState by settingsVm.state.collectAsState()
            val dark = when (themeState.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
            }
            UsageTrackerTheme(darkTheme = dark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    UsageTrackerNavHost(app.viewModelFactory)
                }
            }
        }
    }
}
