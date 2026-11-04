package com.usagetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.usagetracker.app.data.local.entity.LaunchLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LaunchLimitDao {

    @Query("SELECT * FROM launch_limits ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<LaunchLimitEntity>>

    @Query("SELECT * FROM launch_limits WHERE enabled = 1")
    suspend fun getEnabled(): List<LaunchLimitEntity>

    @Query("SELECT * FROM launch_limits WHERE packageName = :pkg LIMIT 1")
    suspend fun get(pkg: String): LaunchLimitEntity?

    @Upsert
    suspend fun upsert(item: LaunchLimitEntity)

    @Query("DELETE FROM launch_limits WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("DELETE FROM launch_limits")
    suspend fun clear()
}
