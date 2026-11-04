package com.usagetracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.usagetracker.app.data.local.dao.AppInfoDao
import com.usagetracker.app.data.local.dao.BlockedAppDao
import com.usagetracker.app.data.local.dao.DailyUsageDao
import com.usagetracker.app.data.local.dao.LaunchLimitDao
import com.usagetracker.app.data.local.dao.UsageSessionDao
import com.usagetracker.app.data.local.entity.AppInfoEntity
import com.usagetracker.app.data.local.entity.BlockedAppEntity
import com.usagetracker.app.data.local.entity.DailyUsageEntity
import com.usagetracker.app.data.local.entity.LaunchLimitEntity
import com.usagetracker.app.data.local.entity.UsageSessionEntity

/**
 * The single Room database backing Usage Tracker.
 *
 * Tables (matches AAB evidence):
 *   - app_info
 *   - usage_sessions
 *   - daily_usage
 *   - blocked_apps
 *   - launch_limits
 *
 * INFERRED — NOT VERIFIED: exact column names / types not recovered.
 */
@Database(
    entities = [
        AppInfoEntity::class,
        UsageSessionEntity::class,
        DailyUsageEntity::class,
        BlockedAppEntity::class,
        LaunchLimitEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UsageDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun launchLimitDao(): LaunchLimitDao

    companion object {
        const val DB_NAME = "usage_tracker.db"

        @Volatile private var INSTANCE: UsageDatabase? = null

        fun get(context: Context): UsageDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context).also { INSTANCE = it }
        }

        private fun build(context: Context): UsageDatabase = Room.databaseBuilder(
            context.applicationContext,
            UsageDatabase::class.java,
            DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
