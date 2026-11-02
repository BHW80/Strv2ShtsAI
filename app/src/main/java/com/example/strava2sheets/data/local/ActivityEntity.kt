package com.example.strava2sheets.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey
    val activityId: Long,
    val startDatum: String,
    val startUhrzeit: String,
    val sportart: String,
    val titel: String,
    val istIndoor: Boolean,
    val distanzKm: Double?,
    val bewegungszeitSek: Long,
    val bewegungszeitFormatted: String,
    val hoehenmeterM: Int?,
    val gesamtzeitFormatted: String,
    val avgGeschwindigkeitKmh: Double?,
    val maxGeschwindigkeitKmh: Double?,
    val avgCadence: Int?,
    val maxCadence: Int? = null,
    val avgHF: Int?,
    val maxHF: Int?,
    val avgLeistungWatt: Int?,
    val npleistungWatt: Int?,
    val energieKj: Int?,
    val kalorienKcal: Int? = null,
    val sufferScore: Int?,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts the 22 target columns of this activity into a list suitable
     * for Google Sheets API v4 append calls. Null values are mapped to empty strings.
     */
    fun toGoogleSheetsRow(): List<Any> {
        return listOf(
            activityId,
            startDatum,
            startUhrzeit,
            sportart,
            titel,
            if (istIndoor) "TRUE" else "FALSE",
            distanzKm ?: "",
            bewegungszeitSek,
            bewegungszeitFormatted,
            hoehenmeterM ?: "",
            gesamtzeitFormatted,
            avgGeschwindigkeitKmh ?: "",
            maxGeschwindigkeitKmh ?: "",
            avgCadence ?: "",
            maxCadence ?: "",
            avgHF ?: "",
            maxHF ?: "",
            avgLeistungWatt ?: "",
            npleistungWatt ?: "",
            energieKj ?: "",
            kalorienKcal ?: "",
            sufferScore ?: ""
        )
    }
}
