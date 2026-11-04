package com.usagetracker.app.domain.util

import com.usagetracker.app.domain.model.AppUsageInfo

enum class AppSortMode {
    MOST_USED, MOST_OPENED, NAME
}

/**
 * INFERRED — NOT VERIFIED.
 */
object AppListSorter {
    fun sort(items: List<AppUsageInfo>, mode: AppSortMode): List<AppUsageInfo> = when (mode) {
        AppSortMode.MOST_USED   -> items.sortedByDescending { it.totalDurationMs }
        AppSortMode.MOST_OPENED -> items.sortedWith(
            compareByDescending<AppUsageInfo> { it.openCount }
                .thenByDescending { it.totalDurationMs }
        )
        AppSortMode.NAME        -> items.sortedBy { it.label.lowercase() }
    }
}
