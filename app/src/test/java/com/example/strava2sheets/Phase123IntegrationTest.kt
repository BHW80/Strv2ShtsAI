package com.example.strava2sheets

import com.example.strava2sheets.data.remote.NetworkClient
import com.example.strava2sheets.data.remote.dto.StravaActivityDto
import com.example.strava2sheets.data.remote.dto.ValueRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class Phase123IntegrationTest {

    @Test
    fun `full data flow from Strava JSON response to Room Entity to Google Sheets ValueRange`() {
        // 1. Simulate Strava API JSON response (Phase 3)
        val stravaJson = """
            [
                {
                    "id": 123456789,
                    "name": "Morning Ride",
                    "distance": 32100.0,
                    "moving_time": 4500,
                    "elapsed_time": 4800,
                    "total_elevation_gain": 450.0,
                    "type": "Ride",
                    "sport_type": "MountainBikeRide",
                    "start_date": "2026-03-29T08:00:00Z",
                    "start_date_local": "2026-03-29T08:00:00Z",
                    "trainer": false,
                    "commute": false,
                    "average_speed": 7.133,
                    "max_speed": 13.888,
                    "average_cadence": 82.0,
                    "max_cadence": 115.0,
                    "average_heartrate": 152.0,
                    "max_heartrate": 178.0,
                    "average_watts": 210.0,
                    "weighted_average_watts": 225.0,
                    "kilojoules": 945.0,
                    "calories": 880.0,
                    "suffer_score": 95.0
                }
            ]
        """.trimIndent()

        // 2. Deserialize JSON with NetworkClient kotlinx.serialization (Phase 3)
        val dtos = NetworkClient.json.decodeFromString<List<StravaActivityDto>>(stravaJson)
        assertNotNull(dtos)
        assertEquals(1, dtos.size)

        val dto = dtos[0]
        assertEquals("Morning Ride", dto.name)
        assertEquals(880.0, dto.calories)
        assertEquals(115.0, dto.maxCadence)

        // 3. Convert DTO to Room Entity (Phase 2 + Phase 3)
        val entity = dto.toEntity()
        assertEquals(123456789L, entity.activityId)
        assertEquals("2026-03-29", entity.startDatum)
        assertEquals("08:00:00", entity.startUhrzeit)
        assertEquals("MountainBikeRide", entity.sportart)
        assertEquals(32.1, entity.distanzKm!!, 0.01)
        assertEquals("01:15:00", entity.bewegungszeitFormatted)
        assertEquals(450, entity.hoehenmeterM)
        assertEquals("01:20:00", entity.gesamtzeitFormatted)
        assertEquals(25.68, entity.avgGeschwindigkeitKmh!!, 0.01)
        assertEquals(50.0, entity.maxGeschwindigkeitKmh!!, 0.01)
        assertEquals(82, entity.avgCadence)
        assertEquals(115, entity.maxCadence)
        assertEquals(152, entity.avgHF)
        assertEquals(178, entity.maxHF)
        assertEquals(210, entity.avgLeistungWatt)
        assertEquals(225, entity.npleistungWatt)
        assertEquals(945, entity.energieKj)
        assertEquals(880, entity.kalorienKcal)
        assertEquals(95, entity.sufferScore)

        // 4. Transform Entity into Google Sheets Row format (Phase 2)
        val rowList = entity.toGoogleSheetsRow()
        assertEquals(22, rowList.size)

        // 5. Wrap into Google Sheets ValueRange payload (Phase 3)
        val valueRange = ValueRange(
            range = "A1",
            majorDimension = "ROWS",
            values = listOf(rowList.map { it.toString() })
        )

        assertNotNull(valueRange)
        assertEquals(1, valueRange.rows.size)
        assertEquals(22, valueRange.rows[0].size)
        assertEquals("123456789", valueRange.rows[0][0])
        assertEquals("2026-03-29", valueRange.rows[0][1])
        assertEquals("MountainBikeRide", valueRange.rows[0][3])
        assertEquals("Morning Ride", valueRange.rows[0][4])
        assertEquals("FALSE", valueRange.rows[0][5])
        assertEquals("32.1", valueRange.rows[0][6])
        assertEquals("01:15:00", valueRange.rows[0][8])
        assertEquals("115", valueRange.rows[0][14])
        assertEquals("880", valueRange.rows[0][20])
    }
}
