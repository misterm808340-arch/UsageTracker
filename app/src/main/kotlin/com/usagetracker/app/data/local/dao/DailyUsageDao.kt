package com.usagetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.usagetracker.app.data.local.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Upsert
    suspend fun upsertAll(items: List<DailyUsageEntity>)

    @Query("SELECT * FROM daily_usage WHERE epochDay = :day")
    suspend fun forDay(day: Int): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE epochDay BETWEEN :from AND :to")
    suspend fun forRange(from: Int, to: Int): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE epochDay BETWEEN :from AND :to")
    suspend fun forDayRange(from: Int, to: Int): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE epochDay = :day ORDER BY totalDurationMs DESC")
    fun observeForDay(day: Int): Flow<List<DailyUsageEntity>>

    @Query("SELECT MAX(updatedAtMs) FROM daily_usage")
    suspend fun latestUpdate(): Long?

    @Query("DELETE FROM daily_usage")
    suspend fun clear()
}
