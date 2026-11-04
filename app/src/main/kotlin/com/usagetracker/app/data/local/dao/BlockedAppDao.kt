package com.usagetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.usagetracker.app.data.local.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {

    @Query("SELECT * FROM blocked_apps ORDER BY startedAtMs DESC")
    fun observeAll(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAll(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun get(pkg: String): BlockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("DELETE FROM blocked_apps WHERE expiresAtMs <= :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM blocked_apps")
    suspend fun clear()
}
