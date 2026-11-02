package com.example.strava2sheets.data.remote.dto

import com.example.strava2sheets.core.util.DataFormatter
import com.example.strava2sheets.data.local.ActivityEntity
import com.example.strava2sheets.data.local.SyncStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class StravaTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: Long,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null
)

object StravaStreamParser {
    /**
     * Extracts calories from the raw DetailedActivity JSON response.
     * Strava returns "calories" (float/number), or sometimes "kilojoules" / "calories_burned".
     */
    fun extractCalories(element: JsonElement): Int? {
        if (element !is JsonObject) return null
        val cal = element["calories"]?.jsonPrimitive?.doubleOrNull
            ?: element["kilocalories"]?.jsonPrimitive?.doubleOrNull
            ?: element["kilojoules"]?.jsonPrimitive?.doubleOrNull
        return cal?.toInt()
    }

    /**
     * Extracts max cadence from either DetailedActivity JSON directly or from the Cadence Stream.
     * 1) Checks for max_cadence inside the object or laps
     * 2) Checks the stream: JsonObject {"cadence": {"data": [...]}} or JsonArray [{"type": "cadence", ...}]
     */
    fun extractMaxCadence(element: JsonElement): Int? {
        when (element) {
            is JsonObject -> {
                // If it's a stream set: {"cadence": {"data": [...]}}
                val streamCadence = element["cadence"]?.jsonObject?.get("data")?.jsonArray
                if (streamCadence != null) {
                    val maxVal = streamCadence.mapNotNull { it.jsonPrimitive.doubleOrNull }.maxOrNull()?.toInt()
                    if (maxVal != null && maxVal > 0) return maxVal
                }

                // If it's DetailedActivity with max_cadence
                val directMax = element["max_cadence"]?.jsonPrimitive?.doubleOrNull?.toInt()
                if (directMax != null && directMax > 0) return directMax

                // Check in laps if present
                val laps = element["laps"]?.let { if (it is JsonArray) it else null }
                if (laps != null) {
                    val maxFromLaps = laps.mapNotNull {
                        (it as? JsonObject)?.get("max_cadence")?.jsonPrimitive?.doubleOrNull?.toInt()
                    }.maxOrNull()
                    if (maxFromLaps != null && maxFromLaps > 0) return maxFromLaps
                }
            }
            is JsonArray -> {
                // If it's stream array: [{"type": "cadence", "data": [...]}]
                val cadenceObj = element.firstOrNull { item ->
                    item is JsonObject && item["type"]?.jsonPrimitive?.content == "cadence"
                } as? JsonObject
                val cadenceData = cadenceObj?.get("data")?.jsonArray
                if (cadenceData != null) {
                    val maxVal = cadenceData.mapNotNull { it.jsonPrimitive.doubleOrNull }.maxOrNull()?.toInt()
                    if (maxVal != null && maxVal > 0) return maxVal
                }
            }
            else -> return null
        }
        return null
    }
}

@Serializable
data class StravaActivityDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("distance") val distance: Double? = null,
    @SerialName("moving_time") val movingTime: Long = 0L,
    @SerialName("elapsed_time") val elapsedTime: Long = 0L,
    @SerialName("total_elevation_gain") val totalElevationGain: Double? = null,
    @SerialName("type") val type: String = "Activity",
    @SerialName("sport_type") val sportType: String? = null,
    @SerialName("start_date") val startDate: String = "",
    @SerialName("start_date_local") val startDateLocal: String = "",
    @SerialName("trainer") val trainer: Boolean? = false,
    @SerialName("commute") val commute: Boolean? = false,
    @SerialName("average_speed") val averageSpeed: Double? = null,
    @SerialName("max_speed") val maxSpeed: Double? = null,
    @SerialName("average_cadence") val averageCadence: Double? = null,
    @SerialName("max_cadence") val maxCadence: Double? = null,
    @SerialName("average_heartrate") val averageHeartrate: Double? = null,
    @SerialName("max_heartrate") val maxHeartrate: Double? = null,
    @SerialName("average_watts") val averageWatts: Double? = null,
    @SerialName("weighted_average_watts") val weightedAverageWatts: Double? = null,
    @SerialName("kilojoules") val kilojoules: Double? = null,
    @SerialName("calories") val calories: Double? = null,
    @SerialName("suffer_score") val sufferScore: Double? = null
) {
    fun toEntity(): ActivityEntity {
        val isoDateToUse = if (startDateLocal.isNotBlank()) startDateLocal else startDate
        val dateFormatted = DataFormatter.parseDate(isoDateToUse)
        val timeFormatted = DataFormatter.parseTime(isoDateToUse)

        val sportNameToUse = sportType ?: type
        val isIndoor = trainer ?: false

        val resolvedCalories = calories?.toInt() ?: kilojoules?.toInt()
        val resolvedKilojoules = kilojoules?.toInt() ?: calories?.toInt()

        return ActivityEntity(
            activityId = id,
            startDatum = dateFormatted,
            startUhrzeit = timeFormatted,
            sportart = sportNameToUse,
            titel = name,
            istIndoor = isIndoor,
            distanzKm = DataFormatter.metersToKm(distance),
            bewegungszeitSek = movingTime,
            bewegungszeitFormatted = DataFormatter.formatSecondsToHms(movingTime),
            hoehenmeterM = totalElevationGain?.toInt(),
            gesamtzeitFormatted = DataFormatter.formatSecondsToHms(elapsedTime),
            avgGeschwindigkeitKmh = DataFormatter.metersPerSecToKmh(averageSpeed),
            maxGeschwindigkeitKmh = DataFormatter.metersPerSecToKmh(maxSpeed),
            avgCadence = averageCadence?.toInt(),
            maxCadence = maxCadence?.toInt(),
            avgHF = averageHeartrate?.toInt(),
            maxHF = maxHeartrate?.toInt(),
            avgLeistungWatt = averageWatts?.toInt(),
            npleistungWatt = weightedAverageWatts?.toInt(),
            energieKj = resolvedKilojoules,
            kalorienKcal = resolvedCalories,
            sufferScore = sufferScore?.toInt(),
            syncStatus = SyncStatus.PENDING,
            fetchedAt = System.currentTimeMillis()
        )
    }
}
