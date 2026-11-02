package com.example.strava2sheets.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object DataFormatter {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun parseDate(isoDateTime: String?): String {
        if (isoDateTime.isNullOrBlank()) return ""
        return try {
            val ldt = parseToLocalDateTime(isoDateTime)
            ldt.format(dateFormatter)
        } catch (e: Exception) {
            ""
        }
    }

    fun parseTime(isoDateTime: String?): String {
        if (isoDateTime.isNullOrBlank()) return ""
        return try {
            val ldt = parseToLocalDateTime(isoDateTime)
            ldt.format(timeFormatter)
        } catch (e: Exception) {
            ""
        }
    }

    fun formatSecondsToHms(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }

    fun metersToKm(meters: Double?): Double? {
        if (meters == null) return null
        return (meters / 1000.0 * 100.0).roundToInt() / 100.0
    }

    fun metersPerSecToKmh(mps: Double?): Double? {
        if (mps == null) return null
        return (mps * 3.6 * 100.0).roundToInt() / 100.0
    }

    private fun parseToLocalDateTime(isoDateTime: String): LocalDateTime {
        val clean = isoDateTime.trim().replace("Z", "").replace("+00:00", "")
        return try {
            LocalDateTime.parse(clean)
        } catch (e: Exception) {
            val instant = Instant.parse(isoDateTime)
            LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
        }
    }
}
