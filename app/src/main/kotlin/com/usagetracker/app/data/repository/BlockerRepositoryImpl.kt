package com.usagetracker.app.data.repository

import android.content.Context
import com.usagetracker.app.data.local.UsageDatabase
import com.usagetracker.app.data.local.entity.BlockedAppEntity
import com.usagetracker.app.data.local.entity.LaunchLimitEntity
import com.usagetracker.app.domain.policy.NotificationPolicyController
import kotlinx.coroutines.flow.Flow

/**
 * Blocker state — actively blocked apps + daily launch limits.
 * INFERRED — NOT VERIFIED.
 */
class BlockerRepositoryImpl(
    private val context: Context,
    private val db: UsageDatabase,
) {

    private val npc = NotificationPolicyController(context)

    fun observeBlocked(): Flow<List<BlockedAppEntity>> = db.blockedAppDao().observeAll()
    fun observeLimits(): Flow<List<LaunchLimitEntity>> = db.launchLimitDao().observeAll()

    suspend fun blockApp(packageName: String, label: String, durationMs: Long, focusMode: Boolean) {
        val now = System.currentTimeMillis()
        db.blockedAppDao().upsert(
            BlockedAppEntity(
                packageName = packageName,
                label = label,
                startedAtMs = now,
                expiresAtMs = now + durationMs,
                focusMode = focusMode,
            )
        )
    }

    suspend fun unblock(packageName: String) = db.blockedAppDao().delete(packageName)

    suspend fun expireDue() = db.blockedAppDao().deleteExpired(System.currentTimeMillis())

    suspend fun setLimit(packageName: String, label: String, limit: Int, enabled: Boolean) {
        db.launchLimitDao().upsert(
            LaunchLimitEntity(
                packageName = packageName,
                label = label,
                limit = limit,
                enabled = enabled,
                updatedAtMs = System.currentTimeMillis(),
            )
        )
    }

    suspend fun removeLimit(packageName: String) = db.launchLimitDao().delete(packageName)

    suspend fun activeBlocksFor(pkg: String): BlockedAppEntity? = db.blockedAppDao().get(pkg)

    suspend fun enabledLimits(): List<LaunchLimitEntity> = db.launchLimitDao().getEnabled()

    // DND policy
    fun dndAccessGranted(): Boolean = npc.hasAccess()
    fun enableDnd(): Boolean = npc.enableDnd()
    fun restoreDnd(): Boolean = npc.restoreFilter()
}
