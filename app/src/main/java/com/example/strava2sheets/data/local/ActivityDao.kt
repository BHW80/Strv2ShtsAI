package com.example.strava2sheets.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrIgnoreActivities(activities: List<ActivityEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActivity(activity: ActivityEntity)

    @Query("SELECT * FROM activities WHERE syncStatus = :status ORDER BY activityId ASC LIMIT :limit")
    suspend fun getActivitiesByStatus(status: SyncStatus = SyncStatus.PENDING, limit: Int = 200): List<ActivityEntity>

    @Query("UPDATE activities SET syncStatus = :status WHERE activityId IN (:activityIds)")
    suspend fun updateSyncStatus(activityIds: List<Long>, status: SyncStatus)

    @Query("SELECT MAX(fetchedAt) FROM activities")
    suspend fun getLatestFetchedTimestamp(): Long?

    @Query("SELECT MAX(activityId) FROM activities")
    suspend fun getHighestActivityId(): Long?

    @Query("SELECT * FROM activities ORDER BY activityId DESC")
    fun getAllActivitiesFlow(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY activityId ASC")
    suspend fun getAllActivitiesList(): List<ActivityEntity>

    @Query("SELECT COUNT(*) FROM activities WHERE syncStatus = :status")
    suspend fun getCountByStatus(status: SyncStatus): Int

    @Query("DELETE FROM activities")
    suspend fun clearAll()
}
