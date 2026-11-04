package com.usagetracker.app.ui.apps

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usagetracker.app.data.datasource.AppIconCache
import com.usagetracker.app.domain.model.EpochDay
import com.usagetracker.app.ui.components.DayTotalsChart
import com.usagetracker.app.ui.components.SectionHeader
import com.usagetracker.app.util.TimeFormat

@Composable
fun AppDetailScreen(
    state: AppDetailUiState,
    onLoad: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(state.packageName) { onLoad() }
    val ctx = LocalContext.current
    // v1.2.2-fix: async icon load.
    val bmp by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(
        initialValue = null, state.packageName,
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AppIconCache.get(ctx).getBitmap(state.packageName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (bmp != null) {
                val dIcon = bmp!!
                androidx.compose.foundation.Image(
                    bitmap = dIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                )
            } else {
                Spacer(Modifier.size(40.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        // Big today card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp),
        ) {
            Text(
                "Today",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                TimeFormat.durationHm(state.todayMs),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${state.sessionCount} sessions · ${state.openCount} opens",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Last 30 days")
        // Convert daily history to totals per day
        val dayMap = state.thirtyDayHistory.associateBy { it.epochDay }
        val today = EpochDay.today()
        val totals = (29 downTo 0).map { d ->
            val day = EpochDay.plusDays(today, -d)
            dayMap[day]?.totalDurationMs ?: 0L
        }
        DayTotalsChart(totals = totals)

        Spacer(Modifier.height(20.dp))
        SectionHeader("History")
        if (state.thirtyDayHistory.isEmpty()) {
            Text(
                "No usage recorded for this app in the last 30 days.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        } else {
            state.thirtyDayHistory.sortedByDescending { it.epochDay }.forEach { d ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        TimeFormat.fullDate(EpochDay.toEpochMs(d.epochDay)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        TimeFormat.durationHm(d.totalDurationMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

