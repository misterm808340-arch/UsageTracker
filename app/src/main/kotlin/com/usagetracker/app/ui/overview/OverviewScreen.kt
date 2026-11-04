package com.usagetracker.app.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usagetracker.app.R
import com.usagetracker.app.ads.BannerAd
import com.usagetracker.app.ui.components.AppRow
import com.usagetracker.app.ui.components.DayTotalsChart
import com.usagetracker.app.ui.components.UsageFilterChip
import com.usagetracker.app.ui.components.HourlyChart
import com.usagetracker.app.ui.components.SectionHeader
import com.usagetracker.app.ui.components.TimelineRow
import com.usagetracker.app.ui.components.UsageCard
import com.usagetracker.app.util.TimeFormat

@Composable
fun OverviewScreen(
    state: OverviewUiState,
    onSetRange: (OverviewRange) -> Unit,
    onRefresh: () -> Unit,
    onOpenApp: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Usage Tracker",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onRefresh) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        if (!state.hasUsageAccess) {
            MissingUsageAccess()
            return@Column
        }

        UsageCard(
            totalMs = state.todaysTotalMs,
            sessionCount = state.sessionCount,
            lastUpdatedMs = state.lastUpdatedMs,
        )

        Spacer(Modifier.height(16.dp))

        // Range chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UsageFilterChip("Hourly", state.range == OverviewRange.HOURLY) { onSetRange(OverviewRange.HOURLY) }
            UsageFilterChip("7 days", state.range == OverviewRange.SEVEN_DAYS) { onSetRange(OverviewRange.SEVEN_DAYS) }
            UsageFilterChip("30 days", state.range == OverviewRange.THIRTY_DAYS) { onSetRange(OverviewRange.THIRTY_DAYS) }
        }

        Spacer(Modifier.height(8.dp))
        when (state.range) {
            OverviewRange.HOURLY     -> HourlyChart(items = state.hourly)
            OverviewRange.SEVEN_DAYS -> DayTotalsChart(totals = state.sevenDayTotals)
            OverviewRange.THIRTY_DAYS -> DayTotalsChart(totals = state.thirtyDayTotals)
        }

        if (state.mostUsed.isNotEmpty()) {
            SectionHeader("Most Used Apps")
            val total = state.mostUsed.sumOf { it.totalDurationMs }.coerceAtLeast(1L)
            state.mostUsed.forEachIndexed { i, app ->
                AppRow(
                    packageName = app.packageName,
                    label = app.label,
                    durationMs = app.totalDurationMs,
                    percent = app.sharePercentOf(total),
                    progressColor = if (i == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    onClick = { onOpenApp(app.packageName, app.label) },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        if (state.mostOpened.isNotEmpty()) {
            SectionHeader("Most Opened Apps")
            state.mostOpened.forEach { app ->
                AppRow(
                    packageName = app.packageName,
                    label = app.label,
                    durationMs = app.totalDurationMs,
                    openCount = app.openCount,
                    showProgress = false,
                    onClick = { onOpenApp(app.packageName, app.label) },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        if (state.timeline.isNotEmpty()) {
            SectionHeader("Usage Timeline (latest first)")
            state.timeline.forEach { t ->
                TimelineRow(
                    label = t.label,
                    startMs = t.startTimeMs,
                    endMs = t.endTimeMs,
                    durationMs = t.durationMs,
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        if (state.mostUsed.isEmpty() && state.mostOpened.isEmpty() && state.timeline.isEmpty()) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = "No usage data yet. Tap refresh.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        Spacer(Modifier.height(16.dp))
        BannerAd()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MissingUsageAccess() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Usage Access required",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Usage Tracker needs Usage Access permission to read on-device usage statistics. No data leaves your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = {
            val i = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        }) { Text("Open Settings") }
    }
}
