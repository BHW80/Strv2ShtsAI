package com.example.strava2sheets.ui.fitness

import com.example.strava2sheets.data.local.ActivityEntity
import com.example.strava2sheets.ui.goals.GoalCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.exp
import kotlin.math.roundToInt

enum class FitnessTimeSpan(val label: String, val days: Int?) {
    LAST_MONTH("Letztes Monat", 30),
    LAST_2_MONTHS("Letzte 2 Monate", 60),
    LAST_6_MONTHS("Letzte 6 Monate", 182),
    LAST_YEAR("Letztes Jahr", 365),
    LAST_2_YEARS("Letzte 2 Jahre", 730),
    ALL_TIME("Alles", null)
}

data class DailyFitnessPoint(
    val date: LocalDate,
    val fitness: Double, // CTL (Chronic Training Load - 42 Tage)
    val fatigue: Double, // ATL (Acute Training Load - 7 Tage)
    val form: Double,    // TSB (Training Stress Balance = CTL - ATL)
    val dailyLoad: Double
)

object FitnessEngine {

    /**
     * Berechnet den Banister/Coggan Fitness-, Fatigue- und Form-Verlauf (PMC).
     * @param activities Alle lokalen Aktivitäten
     * @param ftp Optionale FTP für TSS-Schätzung falls kein sufferScore vorliegt
     * @param daysToInclude Anzahl der Tage rückblickend (oder null für alles)
     */
    fun calculateFitnessHistory(
        activities: List<ActivityEntity>,
        ftp: Int? = null,
        daysToInclude: Int? = 182,
        referenceDate: LocalDate = LocalDate.now()
    ): List<DailyFitnessPoint> {
        if (activities.isEmpty()) return emptyList()

        // 1. Gruppiere Aktivitäten nach Kalendertag und ermittle Daily Training Load
        val loadPerDay = mutableMapOf<LocalDate, Double>()
        for (activity in activities) {
            val date = GoalCalculator.parseDateFlexible(activity.startDatum) ?: continue
            val load = calculateActivityLoad(activity, ftp)
            loadPerDay[date] = (loadPerDay[date] ?: 0.0) + load
        }

        val sortedDates = loadPerDay.keys.sorted()
        if (sortedDates.isEmpty()) return emptyList()

        val earliestDate = sortedDates.first()
        val displayCutoff = if (daysToInclude != null) referenceDate.minusDays(daysToInclude.toLong()) else earliestDate
        val warmupDays = 60L
        val startDate = if (earliestDate.isBefore(displayCutoff.minusDays(warmupDays))) {
            displayCutoff.minusDays(warmupDays)
        } else {
            earliestDate
        }

        // Exponentielle Glättungsfaktoren:
        // CTL (Fitness) = 42 Tage Zeitkonstante: k_ctl = 1 - exp(-1 / 42)
        // ATL (Fatigue) = 7 Tage Zeitkonstante: k_atl = 1 - exp(-1 / 7)
        val kCtl = 1.0 - exp(-1.0 / 42.0)
        val kAtl = 1.0 - exp(-1.0 / 7.0)

        var currentCtl = 0.0
        var currentAtl = 0.0

        val points = mutableListOf<DailyFitnessPoint>()

        var d = startDate

        while (!d.isAfter(referenceDate)) {
            val dailyLoad = loadPerDay[d] ?: 0.0

            currentCtl = currentCtl + (dailyLoad - currentCtl) * kCtl
            currentAtl = currentAtl + (dailyLoad - currentAtl) * kAtl
            val currentTsb = currentCtl - currentAtl

            if (!d.isBefore(displayCutoff)) {
                points.add(
                    DailyFitnessPoint(
                        date = d,
                        fitness = (currentCtl * 10.0).roundToInt() / 10.0,
                        fatigue = (currentAtl * 10.0).roundToInt() / 10.0,
                        form = (currentTsb * 10.0).roundToInt() / 10.0,
                        dailyLoad = (dailyLoad * 10.0).roundToInt() / 10.0
                    )
                )
            }

            d = d.plusDays(1)
        }

        return points
    }

    /**
     * Ermittelt den Trainingsstress (Load / TSS) einer Aktivität.
     */
    private fun calculateActivityLoad(activity: ActivityEntity, ftp: Int?): Double {
        if (activity.sufferScore != null && activity.sufferScore > 0) {
            return activity.sufferScore.toDouble()
        }

        val seconds = activity.bewegungszeitSek.toDouble().coerceAtLeast(0.0)
        val hours = seconds / 3600.0

        // Falls Watt und FTP vorhanden: TSS = (sec * NP/AvgWatts * IF) / (FTP * 3600) * 100
        val watts = (activity.npleistungWatt ?: activity.avgLeistungWatt)?.toDouble()
        if (watts != null && watts > 0 && ftp != null && ftp > 0) {
            val intensityFactor = watts / ftp.toDouble()
            val tss = (seconds * watts * intensityFactor) / (ftp.toDouble() * 3600.0) * 100.0
            return tss.coerceIn(5.0, 500.0)
        }

        // Fallback-Schätzung basierend auf Dauer und Sportart (typischer TSS/h)
        val tssPerHour = when (activity.sportart.lowercase()) {
            "ride", "virtualride" -> 50.0
            "mountainbikeride", "gravelride" -> 60.0
            "run", "virtualrun" -> 70.0
            "rowing", "virtualrow" -> 65.0
            "walk", "hike" -> 30.0
            else -> 45.0
        }

        return (hours * tssPerHour).coerceIn(0.0, 400.0)
    }
}
