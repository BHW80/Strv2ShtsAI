package com.example.strava2sheets.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AthleteProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: AthleteProfileEntity)

    @Query("SELECT * FROM athlete_profiles ORDER BY date DESC LIMIT 1")
    fun getLatestProfileFlow(): Flow<AthleteProfileEntity?>

    @Query("SELECT * FROM athlete_profiles ORDER BY date DESC LIMIT 1")
    suspend fun getLatestProfile(): AthleteProfileEntity?

    @Query("SELECT * FROM athlete_profiles ORDER BY date ASC")
    fun getAllProfilesHistoryFlow(): Flow<List<AthleteProfileEntity>>

    @Query("SELECT * FROM athlete_profiles ORDER BY date ASC")
    suspend fun getAllProfilesHistory(): List<AthleteProfileEntity>

    @Query("DELETE FROM athlete_profiles WHERE date = :date")
    suspend fun deleteProfileByDate(date: String)
}
