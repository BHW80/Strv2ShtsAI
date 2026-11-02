package com.example.strava2sheets.data.repository

import android.util.Log
import com.example.strava2sheets.data.local.AthleteProfileDao
import com.example.strava2sheets.data.local.AthleteProfileEntity
import com.example.strava2sheets.data.remote.api.StravaApiService
import com.example.strava2sheets.data.remote.auth.StravaAuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class AthleteRepository(
    private val athleteProfileDao: AthleteProfileDao,
    private val stravaApiService: StravaApiService,
    private val stravaAuthManager: StravaAuthManager
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val latestProfile: Flow<AthleteProfileEntity?> = athleteProfileDao.getLatestProfileFlow()
    val profilesHistory: Flow<List<AthleteProfileEntity>> = athleteProfileDao.getAllProfilesHistoryFlow()

    suspend fun fetchAndSaveAthleteProfile(): Result<AthleteProfileEntity> {
        val token = stravaAuthManager.getValidAccessToken()
            ?: return Result.failure(IllegalStateException("Strava nicht authentifiziert oder Token abgelaufen. Bitte auf Start 'Mit Strava verbinden' tippen."))

        return try {
            val authHeader = "Bearer $token"

            // 1. Athlet abrufen (Gewicht, FTP, Name, Bild)
            Log.d("AthleteRepository", "Abrufen von GET /athlete...")
            val athleteDto = stravaApiService.getAuthenticatedAthlete(authHeader)
            Log.d("AthleteRepository", "GET /athlete erfolgreich: ID=${athleteDto.id}, Name=${athleteDto.firstname}, Gewicht=${athleteDto.weight}, FTP=${athleteDto.ftp}")

            // 2. Zonen abrufen (HF- und Watt-Zonen) - separat abgesichert
            val zonesDto = try {
                stravaApiService.getAthleteZones(authHeader)
            } catch (e: Exception) {
                Log.w("AthleteRepository", "Konnte Zonen nicht abrufen (optional): ${e.message}")
                null
            }

            // 3. FTP und W/kg berechnen falls vorhanden
            val weight = athleteDto.weight
            val ftpInt = athleteDto.ftp?.toInt()
            val wPerKg = if (weight != null && weight > 0.0 && ftpInt != null && ftpInt > 0) {
                ((ftpInt.toDouble() / weight) * 100.0).roundToInt() / 100.0
            } else null

            // 4. Zonen als JSON serialisieren
            val hrZonesJson = zonesDto?.heartRate?.zones?.let {
                try {
                    json.encodeToString(it)
                } catch (e: Exception) {
                    Log.w("AthleteRepository", "Fehler beim Serialisieren der HR-Zonen: ${e.message}")
                    null
                }
            }

            val powerZonesJson = zonesDto?.power?.zones?.let {
                try {
                    json.encodeToString(it)
                } catch (e: Exception) {
                    Log.w("AthleteRepository", "Fehler beim Serialisieren der Power-Zonen: ${e.message}")
                    null
                }
            }

            // Heutiges Datum als Primärschlüssel ("yyyy-MM-dd")
            val todayDate = LocalDate.now().format(dateFormatter)

            val profileEntity = AthleteProfileEntity(
                date = todayDate,
                athleteId = athleteDto.id,
                firstname = athleteDto.firstname,
                lastname = athleteDto.lastname,
                profilePictureUrl = athleteDto.profile ?: athleteDto.profileMedium,
                sex = athleteDto.sex,
                weightKg = athleteDto.weight,
                ftpWatts = ftpInt,
                calculatedWPerKg = wPerKg,
                heartRateZonesJson = hrZonesJson,
                powerZonesJson = powerZonesJson,
                fetchedAt = System.currentTimeMillis()
            )

            athleteProfileDao.insertOrUpdateProfile(profileEntity)
            Log.d("AthleteRepository", "Profil erfolgreich in Room gespeichert für Datum: $todayDate")
            Result.success(profileEntity)
        } catch (e: Exception) {
            Log.e("AthleteRepository", "Fehler in fetchAndSaveAthleteProfile: ${e.message}", e)
            Result.failure(e)
        }
    }
}
