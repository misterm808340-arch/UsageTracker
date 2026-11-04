package com.usagetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.usagetracker.app.data.local.entity.UsageSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<UsageSessionEntity>)

    @Query("SELECT * FROM usage_sessions WHERE startTimeMs >= :start AND endTimeMs <= :end ORDER BY startTimeMs ASC")
    suspend fun range(start: Long, end: Long): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions WHERE epochDay = :day ORDER BY startTimeMs ASC")
    suspend fun forDay(day: Int): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions WHERE epochDay BETWEEN :fromDay AND :toDay ORDER BY startTimeMs ASC")
    suspend fun forDayRange(fromDay: Int, toDay: Int): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions ORDER BY startTimeMs ASC")
    fun observeAll(): Flow<List<UsageSessionEntity>>

    @Query("SELECT MAX(endTimeMs) FROM usage_sessions")
    suspend fun latestEndTime(): Long?

    @Query("DELETE FROM usage_sessions WHERE endTimeMs < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM usage_sessions")
    suspend fun clear()
}
