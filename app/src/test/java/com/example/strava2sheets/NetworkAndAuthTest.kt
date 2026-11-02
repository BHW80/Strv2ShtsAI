package com.example.strava2sheets

import com.example.strava2sheets.data.remote.auth.GoogleAuthManager
import com.example.strava2sheets.data.remote.dto.StravaActivityDto
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkAndAuthTest {

    @Test
    fun `extractSpreadsheetId extracts ID from full Google Sheets URL`() {
        val authManager = GoogleAuthManager()

        val fullUrl = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit#gid=0"
        val extractedId = authManager.extractSpreadsheetId(fullUrl)

        assertEquals("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms", extractedId)
    }

    @Test
    fun `extractSpreadsheetId keeps plain ID intact`() {
        val authManager = GoogleAuthManager()

        val plainId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
        val extractedId = authManager.extractSpreadsheetId(plainId)

        assertEquals("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms", extractedId)
    }

    @Test
    fun `StravaActivityDto converts correctly to ActivityEntity`() {
        val dto = StravaActivityDto(
            id = 99887766L,
            name = "Lunch Ride",
            distance = 25000.0, // 25 km
            movingTime = 3600L, // 1 hour
            elapsedTime = 3900L,
            totalElevationGain = 200.0,
            type = "Ride",
            startDateLocal = "2026-03-29T12:00:00Z",
            averageSpeed = 6.944, // m/s ~ 25 km/h
            maxSpeed = 12.5, // m/s = 45 km/h
            averageHeartrate = 145.0,
            maxHeartrate = 170.0
        )

        val entity = dto.toEntity()

        assertEquals(99887766L, entity.activityId)
        assertEquals("2026-03-29", entity.startDatum)
        assertEquals("12:00:00", entity.startUhrzeit)
        assertEquals("Ride", entity.sportart)
        assertEquals("Lunch Ride", entity.titel)
        assertEquals(25.0, entity.distanzKm!!, 0.01)
        assertEquals(3600L, entity.bewegungszeitSek)
        assertEquals("01:00:00", entity.bewegungszeitFormatted)
        assertEquals(200, entity.hoehenmeterM)
        assertEquals(25.0, entity.avgGeschwindigkeitKmh!!, 0.1)
        assertEquals(45.0, entity.maxGeschwindigkeitKmh!!, 0.1)
        assertEquals(145, entity.avgHF)
        assertEquals(170, entity.maxHF)
    }
}
