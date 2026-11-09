package com.usagetracker.app.ui.settings

import android.content.Context
import android.content.Intent
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usagetracker.app.BuildConfig
import com.usagetracker.app.ads.BannerAd
import com.usagetracker.app.ads.RewardedAdManager
import com.usagetracker.app.data.datastore.ThemeMode
import com.usagetracker.app.ui.components.SectionHeader

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSetTheme: (ThemeMode) -> Unit,
    onSetShowSystemApps: (Boolean) -> Unit,
    onRefreshUsage: () -> Unit,
    onClearCache: () -> Unit,
    onDeleteAll: () -> Unit,
    onExportCsv: () -> Unit,
    onToastShown: () -> Unit,
) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
            onToastShown()
        }
    }
    LaunchedEffect(state.csvUri) {
        state.csvUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(Intent.createChooser(intent, "Export CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))

        // Usage Access
        SectionHeader("Usage Access")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    if (state.hasUsageAccess) "Usage access granted" else "Usage access required to read usage stats",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = {
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Manage Usage Access") }
            }
        }

        // Appearance
        SectionHeader("Appearance")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                ThemeRadioButton("System default", state.theme == ThemeMode.SYSTEM) { onSetTheme(ThemeMode.SYSTEM) }
                ThemeRadioButton("Light",         state.theme == ThemeMode.LIGHT)  { onSetTheme(ThemeMode.LIGHT) }
                ThemeRadioButton("Dark",          state.theme == ThemeMode.DARK)   { onSetTheme(ThemeMode.DARK) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show system apps in lists",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("Include launcher, settings, and other system packages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.showSystemApps,
                    onCheckedChange = onSetShowSystemApps,
                )
            }
        }

        // Data
        SectionHeader("Data")
        SettingsActionRow(
            icon = Icons.Outlined.Cached,
            title = "Refresh usage data",
            subtitle = if (state.refreshing) "Refreshing…" else "Re-pull last 7 days from system",
            onClick = onRefreshUsage,
            enabled = !state.refreshing,
        )
        SettingsActionRow(
            icon = Icons.Outlined.Download,
            title = "Export data (CSV)",
            subtitle = "Save a CSV of the last 30 days",
            onClick = onExportCsv,
        )
        SettingsActionRow(
            icon = Icons.Outlined.Delete,
            title = "Clear cached data",
            subtitle = "Drop usage_sessions and daily_usage rows",
            onClick = onClearCache,
        )
        SettingsActionRow(
            icon = Icons.Outlined.Delete,
            title = "Delete all data",
            subtitle = "Wipe every table including app_info",
            onClick = { confirmDelete = true },
            tint = MaterialTheme.colorScheme.error,
        )

        // About
        SectionHeader("About")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(0.dp))
                Spacer(Modifier.padding(horizontal = 8.dp))
                Column {
                    Text("Usage Tracker",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium)
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Usage data stays on your device. Ads are delivered by Google AdMob.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Ads section: disclose which ad networks are used, link to privacy policy.
        SectionHeader("Ads")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("This app shows ads served by Google AdMob.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text("AdMob and its partners may use device identifiers and usage data to serve " +
                    "relevant ads. Usage data collected by this app for its own charts and " +
                    "history is never sent to ad networks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Ad unit mode: ${if (BuildConfig.DEBUG) "Test ads (debug build)" else "Live ads (release)"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Optional rewarded ad — strictly opt-in, never auto-shown.
                Spacer(Modifier.height(12.dp))
                var rewardToast by remember { mutableStateOf<String?>(null) }
                val ctx = LocalContext.current
                Button(
                    onClick = {
                        val act = ctx.findActivity()
                        if (act != null) {
                            RewardedAdManager.show(act) { amount ->
                                rewardToast = "Thanks! You earned $amount reward."
                            }
                        }
                    },
                    enabled = RewardedAdManager.isReady(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Watch a short ad to support us")
                }
                LaunchedEffect(rewardToast) {
                    rewardToast?.let {
                        Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
                        rewardToast = null
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        BannerAd()
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(onClick = { onDeleteAll(); confirmDelete = false }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
            title = { Text("Delete all data?") },
            text = { Text("This permanently wipes all usage history, blocked apps, and limits. Cannot be undone.") }
        )
    }
}

@Composable
private fun ThemeRadioButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.padding(horizontal = 6.dp))
        Text(label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.padding(horizontal = 8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Walks up the Context chain to find the host Activity, if any. */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
