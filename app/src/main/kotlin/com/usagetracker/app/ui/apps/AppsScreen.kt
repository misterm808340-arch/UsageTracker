package com.usagetracker.app.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.usagetracker.app.domain.util.AppSortMode
import com.usagetracker.app.ui.components.AppRow
import com.usagetracker.app.ui.components.UsageFilterChip

@Composable
fun AppsScreen(
    state: AppsUiState,
    onQuery: (String) -> Unit,
    onPeriod: (AppsPeriod) -> Unit,
    onSort: (AppSortMode) -> Unit,
    onRefresh: () -> Unit,
    onAppClick: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Apps",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onRefresh) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UsageFilterChip("Today", state.period == AppsPeriod.TODAY) { onPeriod(AppsPeriod.TODAY) }
            UsageFilterChip("Yesterday", state.period == AppsPeriod.YESTERDAY) { onPeriod(AppsPeriod.YESTERDAY) }
            UsageFilterChip("7 days", state.period == AppsPeriod.SEVEN_DAYS) { onPeriod(AppsPeriod.SEVEN_DAYS) }
            UsageFilterChip("30 days", state.period == AppsPeriod.THIRTY_DAYS) { onPeriod(AppsPeriod.THIRTY_DAYS) }
        }

        // Sort dropdown
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var sortOpen by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { sortOpen = true }) {
                    Text(
                        text = when (state.sortMode) {
                            AppSortMode.MOST_USED -> "Sort: Most used"
                            AppSortMode.MOST_OPENED -> "Sort: Most opened"
                            AppSortMode.NAME -> "Sort: Name"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    DropdownMenuItem(text = { Text("Most used") }, onClick = { onSort(AppSortMode.MOST_USED); sortOpen = false })
                    DropdownMenuItem(text = { Text("Most opened") }, onClick = { onSort(AppSortMode.MOST_OPENED); sortOpen = false })
                    DropdownMenuItem(text = { Text("Name") }, onClick = { onSort(AppSortMode.NAME); sortOpen = false })
                }
            }
        }

        if (state.items.isEmpty() && !state.loading) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "No apps to show for this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.items, key = { it.packageName }) { app ->
                AppRow(
                    packageName = app.packageName,
                    label = app.label,
                    durationMs = app.totalDurationMs,
                    openCount = app.openCount,
                    percent = null,
                    showProgress = false,
                    onClick = { onAppClick(app.packageName, app.label) },
                )
            }
            item {
                Text(
                    text = "Open counts are estimated from Android usage events and may not match exactly.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            item {
                BannerAd(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }
}
