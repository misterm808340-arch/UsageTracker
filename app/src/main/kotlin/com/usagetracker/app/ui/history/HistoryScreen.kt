package com.usagetracker.app.ui.history

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usagetracker.app.ads.BannerAd
import com.usagetracker.app.domain.model.EpochDay
import com.usagetracker.app.ui.components.AppRow
import com.usagetracker.app.ui.components.SectionHeader
import com.usagetracker.app.ui.components.TimelineRow
import com.usagetracker.app.util.TimeFormat
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDay: (Int) -> Unit,
    onOpenApp: (String, String) -> Unit,
) {
    var dateDialogOpen by remember { mutableStateOf(false) }
    val tz = TimeZone.getDefault()
    val dayLabel = TimeFormat.fullDate(EpochDay.toEpochMs(state.epochDay, tz))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevDay) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous day",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                dayLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { dateDialogOpen = true },
            )
            IconButton(onClick = onNextDay) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next day",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Summary card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Total usage",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                Text(TimeFormat.durationHm(state.totalMs),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${state.sessionCount} app sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }

        if (state.ranked.isNotEmpty()) {
            SectionHeader("Top apps")
            val total = state.ranked.sumOf { it.totalDurationMs }.coerceAtLeast(1L)
            state.ranked.take(8).forEachIndexed { i, app ->
                AppRow(
                    packageName = app.packageName,
                    label = app.label,
                    durationMs = app.totalDurationMs,
                    percent = app.sharePercentOf(total),
                    progressColor = if (i < 3) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    onClick = { onOpenApp(app.packageName, app.label) },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        if (state.ranked.isNotEmpty()) {
            SectionHeader("App breakdown")
            val total = state.ranked.sumOf { it.totalDurationMs }.coerceAtLeast(1L)
            state.ranked.forEach { app ->
                AppRow(
                    packageName = app.packageName,
                    label = app.label,
                    durationMs = app.totalDurationMs,
                    percent = app.sharePercentOf(total),
                    onClick = { onOpenApp(app.packageName, app.label) },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        if (state.timeline.isNotEmpty()) {
            SectionHeader("Timeline")
            state.timeline.forEach { t ->
                TimelineRow(
                    label = t.label,
                    startMs = t.startTimeMs,
                    endMs = t.endTimeMs,
                    durationMs = t.durationMs,
                    packageName = t.packageName,
                    onClick = { onOpenApp(t.packageName, t.label) },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        if (state.ranked.isEmpty() && state.timeline.isEmpty()) {
            Spacer(Modifier.height(40.dp))
            Text(
                "No usage data for this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        BannerAd()
        Spacer(Modifier.height(24.dp))
    }

    if (dateDialogOpen) {
        val initialState = rememberDatePickerState(
            initialSelectedDateMillis = EpochDay.toEpochMs(state.epochDay, tz),
        )
        DatePickerDialog(
            onDismissRequest = { dateDialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = initialState.selectedDateMillis
                    if (ms != null) onPickDay(EpochDay.fromEpochMs(ms, tz))
                    dateDialogOpen = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { dateDialogOpen = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = initialState)
        }
    }
}
