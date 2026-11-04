package com.usagetracker.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.usagetracker.app.UsageTrackerApplication
import com.usagetracker.app.domain.model.DaySlice
import java.util.concurrent.TimeUnit

/**
 * Periodic background sync. Runs every ~6 hours, pulls the last 24h of usage
 * events from UsageStatsManager, runs SessionDetector, persists results.
 *
 * INFERRED — NOT VERIFIED.
 */
class UsageSyncWorker(
    app: Context,
    params: WorkerParameters,
) : CoroutineWorker(app, params) {

    override suspend fun doWork(): Result = try {
        val app = applicationContext as UsageTrackerApplication
        val now = System.currentTimeMillis()
        val startMs = now - TimeUnit.DAYS.toMillis(1)
        app.usageRepo.syncRange(startMs, now)
        app.blockerRepo.expireDue()
        Result.success()
    } catch (t: Throwable) {
        Result.retry()
    }

    companion object {
        const val WORK_NAME = "usage-sync"
    }
}
