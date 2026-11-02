package com.example.strava2sheets

import com.example.strava2sheets.data.local.ActivityEntity
import com.example.strava2sheets.data.local.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityEntityTest {

    @Test
    fun `toGoogleSheetsRow returns exactly 22 elements`() {
        val entity = ActivityEntity(
            activityId = 123456789L,
            startDatum = "2026-03-29",
            startUhrzeit = "08:30:00",
            sportart = "Ride",
            titel = "Morning Ride",
            istIndoor = false,
            distanzKm = 45.2,
            bewegungszeitSek = 5400L,
            bewegungszeitFormatted = "01:30:00",
            hoehenmeterM = 350,
            gesamtzeitFormatted = "01:35:00",
            avgGeschwindigkeitKmh = 30.1,
            maxGeschwindigkeitKmh = 48.5,
            avgCadence = 85,
            maxCadence = 110,
            avgHF = 150,
            maxHF = 175,
            avgLeistungWatt = 220,
            npleistungWatt = 235,
            energieKj = 1200,
            kalorienKcal = 950,
            sufferScore = 80,
            syncStatus = SyncStatus.PENDING
        )

        val row = entity.toGoogleSheetsRow()
        assertEquals(22, row.size)
        assertEquals(123456789L, row[0])
        assertEquals("2026-03-29", row[1])
        assertEquals("08:30:00", row[2])
        assertEquals("Ride", row[3])
        assertEquals("Morning Ride", row[4])
        assertEquals("FALSE", row[5])
        assertEquals(45.2, row[6])
        assertEquals(5400L, row[7])
        assertEquals("01:30:00", row[8])
        assertEquals(350, row[9])
        assertEquals("01:35:00", row[10])
        assertEquals(30.1, row[11])
        assertEquals(48.5, row[12])
        assertEquals(85, row[13])
        assertEquals(110, row[14])
        assertEquals(150, row[15])
        assertEquals(175, row[16])
        assertEquals(220, row[17])
        assertEquals(235, row[18])
        assertEquals(1200, row[19])
        assertEquals(950, row[20])
        assertEquals(80, row[21])
    }

    @Test
    fun `toGoogleSheetsRow replaces null values with empty string`() {
        val entity = ActivityEntity(
            activityId = 987654321L,
            startDatum = "2026-03-29",
            startUhrzeit = "10:00:00",
            sportart = "Run",
            titel = "Trail Run",
            istIndoor = true,
            distanzKm = null,
            bewegungszeitSek = 1800L,
            bewegungszeitFormatted = "00:30:00",
            hoehenmeterM = null,
            gesamtzeitFormatted = "00:30:00",
            avgGeschwindigkeitKmh = null,
            maxGeschwindigkeitKmh = null,
            avgCadence = null,
            maxCadence = null,
            avgHF = null,
            maxHF = null,
            avgLeistungWatt = null,
            npleistungWatt = null,
            energieKj = null,
            kalorienKcal = null,
            sufferScore = null,
            syncStatus = SyncStatus.PENDING
        )

        val row = entity.toGoogleSheetsRow()
        assertEquals(22, row.size)
        assertEquals("TRUE", row[5])
        assertEquals("", row[6])
        assertEquals("", row[9])
        assertEquals("", row[11])
        assertEquals("", row[13])
        assertEquals("", row[14])
        assertEquals("", row[15])
        assertEquals("", row[17])
        assertEquals("", row[20])
    }
}
