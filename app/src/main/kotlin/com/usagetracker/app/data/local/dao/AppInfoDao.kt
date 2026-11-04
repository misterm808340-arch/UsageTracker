package com.usagetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.usagetracker.app.data.local.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {

    @Query("SELECT * FROM app_info ORDER BY label COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_info WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppInfoEntity?

    @Query("SELECT * FROM app_info")
    suspend fun getAll(): List<AppInfoEntity>

    @Upsert
    suspend fun upsert(items: List<AppInfoEntity>)

    @Query("UPDATE app_info SET lastSeenMs = :now WHERE packageName = :pkg")
    suspend fun touchLastSeen(pkg: String, now: Long)

    @Query("DELETE FROM app_info WHERE packageName NOT IN (:keep)")
    suspend fun deleteMissing(keep: List<String>)

    @Query("DELETE FROM app_info")
    suspend fun clear()
}
