package com.example.strava2sheets

import com.example.strava2sheets.core.util.DataFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class DataFormatterTest {

    @Test
    fun `parseDate converts ISO date string to YYYY-MM-DD`() {
        val isoDate = "2026-03-29T08:30:00Z"
        val formatted = DataFormatter.parseDate(isoDate)
        assertEquals("2026-03-29", formatted)
    }

    @Test
    fun `parseTime converts ISO date string to HH-MM-SS`() {
        val isoDate = "2026-03-29T08:30:00Z"
        val formatted = DataFormatter.parseTime(isoDate)
        assertEquals("08:30:00", formatted)
    }

    @Test
    fun `formatSecondsToHms formats seconds into HH-mm-ss`() {
        val seconds = 3661L // 1 hour, 1 min, 1 sec
        val formatted = DataFormatter.formatSecondsToHms(seconds)
        assertEquals("01:01:01", formatted)
    }

    @Test
    fun `metersToKm converts meters to km rounded to 2 decimals`() {
        val meters = 12345.0
        val km = DataFormatter.metersToKm(meters)
        assertEquals(12.35, km!!, 0.001)
    }

    @Test
    fun `metersPerSecToKmh converts m-s to km-h rounded to 2 decimals`() {
        val mps = 10.0 // 10 m/s = 36 km/h
        val kmh = DataFormatter.metersPerSecToKmh(mps)
        assertEquals(36.0, kmh!!, 0.001)
    }
}
