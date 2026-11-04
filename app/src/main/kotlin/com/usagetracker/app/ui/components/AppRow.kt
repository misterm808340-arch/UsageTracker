package com.usagetracker.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usagetracker.app.data.datasource.AppIconCache
import com.usagetracker.app.util.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun UsageCard(
    totalMs: Long,
    sessionCount: Int,
    lastUpdatedMs: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp),
    ) {
        Text(
            text = "Today's Usage",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = TimeFormat.durationHm(totalMs),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$sessionCount app sessions · Last updated ${TimeFormat.clockTime(lastUpdatedMs)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(MaterialTheme.shapes.small),
        color = color,
        trackColor = trackColor,
    )
}

@Composable
fun AppRow(
    packageName: String,
    label: String,
    durationMs: Long,
    openCount: Int? = null,
    percent: Float? = null,
    showProgress: Boolean = true,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    // v1.2.2-fix: icon loads off the main thread (was a blocking PackageManager
    // binder call inside remember{} for every row — main source of list jank).
    val bmp by produceState<android.graphics.Bitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) { AppIconCache.get(ctx).getBitmap(packageName) }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bmp != null) {
            val b = bmp!!
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (percent != null) {
                    Text(
                        text = "${percent.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = TimeFormat.durationShort(durationMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (showProgress && percent != null) {
                Spacer(Modifier.height(6.dp))
                ProgressBar(progress = percent / 100f, color = progressColor)
            }
            if (openCount != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$openCount opens",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun UsageFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
