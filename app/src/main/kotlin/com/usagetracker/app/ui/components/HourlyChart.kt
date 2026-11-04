package com.usagetracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.usagetracker.app.domain.model.HourlyUsage
import com.usagetracker.app.util.TimeFormat

@Composable
fun HourlyChart(
    items: List<HourlyUsage>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    val maxMs = (items.maxOfOrNull { it.totalDurationMs } ?: 0L).coerceAtLeast(1L)
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val barCount = 24
            val totalWidth = size.width
            val gap = 4f
            val barWidth = (totalWidth - gap * (barCount - 1)) / barCount
            val maxH = size.height
            items.forEachIndexed { i, h ->
                val ratio = h.totalDurationMs.toFloat() / maxMs.toFloat()
                val barH = maxH * ratio
                val x = i * (barWidth + gap)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, maxH - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18, 23).forEach { h ->
                Text(
                    text = "${h}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DayTotalsChart(
    totals: List<Long>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (totals.isEmpty()) return
    val max = (totals.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val labels = if (totals.size == 7) listOf("7d","6d","5d","4d","3d","2d","1d")
        else (1..totals.size).map { "${totals.size - it + 1}d" }
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val n = totals.size
            val gap = 4f
            val barWidth = (size.width - gap * (n - 1)) / n
            totals.forEachIndexed { i, v ->
                val ratio = v.toFloat() / max.toFloat()
                val h = size.height * ratio
                val x = i * (barWidth + gap)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            labels.forEach { l ->
                Text(l, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TimelineRow(
    label: String,
    startMs: Long,
    endMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = TimeFormat.clockTime(startMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = TimeFormat.durationShort(durationMs),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
