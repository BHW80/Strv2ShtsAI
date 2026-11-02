package com.example.strava2sheets.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "athlete_profiles")
data class AthleteProfileEntity(
    @PrimaryKey
    val date: String, // "yyyy-MM-dd" - eindeutiger Tagesschlüssel für Upsert
    val athleteId: Long,
    val firstname: String?,
    val lastname: String?,
    val profilePictureUrl: String?,
    val sex: String?,
    val weightKg: Double?,
    val ftpWatts: Int?,
    val calculatedWPerKg: Double?, // z.B. 3.45 W/kg
    val heartRateZonesJson: String?, // JSON Liste der Zonen z.B. [{"min":0,"max":120}, ...]
    val powerZonesJson: String?,     // JSON Liste der Watt-Zonen
    val fetchedAt: Long = System.currentTimeMillis()
)
