package com.example.strava2sheets.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ActivityEntity::class, GoalEntity::class, AthleteProfileEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(DataConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    abstract fun goalDao(): GoalDao
    abstract fun athleteProfileDao(): AthleteProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "strava_sync_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
