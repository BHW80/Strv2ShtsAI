package com.example.strava2sheets.ui.goals

import com.example.strava2sheets.data.local.ActivityEntity
import com.example.strava2sheets.data.local.GoalEntity
import com.example.strava2sheets.data.local.GoalMetricType
import com.example.strava2sheets.data.local.GoalPeriodType
import com.example.strava2sheets.data.local.SeasonalityCurve
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.exp

data class CurvePoint(
    val dayOfYear: Int,
    val expectedRatio: Float, // 0.0f bis 1.0f (Soll)
    val actualRatio: Float?   // 0.0f bis 1.0f (Ist bis heute)
)

data class GoalProgress(
    val goal: GoalEntity,
    val currentValue: Double,
    val targetValue: Double,
    val progressPercent: Double,
    val expectedValueToday: Double,
    val deltaToSchedule: Double, // > 0 = vor dem Plan, < 0 = Rückstand
    val deltaPercent: Double,
    val remainingDays: Int,
    val isAheadOfSchedule: Boolean,
    val formattedCurrent: String,
    val formattedTarget: String,
    val formattedDelta: String,
    val chartPoints: List<CurvePoint> = emptyList()
)

object GoalCalculator {

    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val germanDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun calculateProgress(
        goal: GoalEntity,
        activities: List<ActivityEntity>,
        referenceDate: LocalDate = LocalDate.now()
    ): GoalProgress {
        val (startDate, endDate) = getPeriodDates(goal, referenceDate)

        // Exakte & saubere Sportart-Filterung
        val relevantActivities = activities.filter { entity ->
            val d = parseDateFlexible(entity.startDatum)
            val sportMatches = isSportMatching(entity.sportart, goal.sportart)
            sportMatches && d != null && !d.isBefore(startDate) && !d.isAfter(endDate)
        }

        // Aktueller Ist-Wert
        val currentValue = when (goal.metricType) {
            GoalMetricType.DISTANCE -> relevantActivities.sumOf { it.distanzKm ?: 0.0 }
            GoalMetricType.TIME -> relevantActivities.sumOf { it.bewegungszeitSek }.toDouble() / 3600.0
            GoalMetricType.ELEVATION -> relevantActivities.sumOf { it.hoehenmeterM ?: 0 }.toDouble()
            GoalMetricType.CALORIES -> relevantActivities.sumOf { it.kalorienKcal ?: 0 }.toDouble()
            GoalMetricType.WORKOUTS -> relevantActivities.size.toDouble()
        }

        val (expectedValue, remainingDays) = calculateExpectedValue(goal, startDate, endDate, referenceDate)

        val progressPercent = if (goal.targetValue > 0) {
            ((currentValue / goal.targetValue) * 100.0).coerceAtLeast(0.0)
        } else 0.0

        val deltaToSchedule = currentValue - expectedValue
        val deltaPercent = if (expectedValue > 0) {
            (deltaToSchedule / expectedValue) * 100.0
        } else 0.0

        val isAhead = deltaToSchedule >= 0.0

        val formattedCurrent = formatMetricValue(currentValue, goal.metricType)
        val formattedTarget = formatMetricValue(goal.targetValue, goal.metricType)
        val formattedDelta = "${if (isAhead) "+" else ""}${formatMetricValue(deltaToSchedule, goal.metricType)}"

        // Erstelle Chart-Punkte für den Jahresverlauf (Soll-Kurve vs. Ist-Kurve)
        val chartPoints = if (goal.periodType == GoalPeriodType.YEARLY) {
            buildChartPoints(goal, relevantActivities, referenceDate)
        } else {
            emptyList()
        }

        return GoalProgress(
            goal = goal,
            currentValue = currentValue,
            targetValue = goal.targetValue,
            progressPercent = progressPercent,
            expectedValueToday = expectedValue,
            deltaToSchedule = deltaToSchedule,
            deltaPercent = deltaPercent,
            remainingDays = remainingDays,
            isAheadOfSchedule = isAhead,
            formattedCurrent = formattedCurrent,
            formattedTarget = formattedTarget,
            formattedDelta = formattedDelta,
            chartPoints = chartPoints
        )
    }

    private fun isSportMatching(activitySport: String?, goalSport: String?): Boolean {
        if (goalSport.isNullOrBlank() || goalSport.equals("Alle Sportarten", ignoreCase = true)) {
            return true
        }
        if (activitySport.isNullOrBlank()) return false

        val act = activitySport.trim().lowercase()
        val target = goalSport.trim().lowercase()

        // 1. Exakter Match
        if (act == target) return true

        // 2. Präzise Sportarten-Gruppierung ohne Überschneidungen:
        // - "Ride" matcht ausschließlich normales Rennrad / Rad ("Ride", "VirtualRide")
        // - "MountainBikeRide" oder "MTB" matcht nur Mountainbike
        // - "GravelRide" matcht nur Gravel
        // - "EBikeRide" matcht nur E-Bike
        if (target == "ride") {
            return act == "ride" || act == "virtualride"
        }
        if (target == "virtualride") {
            return act == "virtualride"
        }
        if (target == "mountainbikeride" || target == "mtb") {
            return act == "mountainbikeride" || act.contains("mountain")
        }
        if (target == "gravelride") {
            return act == "gravelride"
        }
        if (target == "ebikeride") {
            return act == "ebikeride"
        }

        // Rudern
        if (target == "rowing") {
            return act == "rowing" || act == "virtualrow"
        }
        if (target == "virtualrow") {
            return act == "virtualrow"
        }

        // Laufen & Walk
        if (target == "run") {
            return act == "run" || act == "virtualrun"
        }
        if (target == "walk") {
            return act == "walk" || act == "hike"
        }

        return false
    }

    private fun buildChartPoints(
        goal: GoalEntity,
        activities: List<ActivityEntity>,
        referenceDate: LocalDate
    ): List<CurvePoint> {
        val targetYear = if (goal.year > 2000) goal.year else referenceDate.year
        val currentDay = if (referenceDate.year == targetYear) referenceDate.dayOfYear else 365
        val points = mutableListOf<CurvePoint>()

        // Aktivitäten nach DayOfYear aggregieren
        val dailyMap = activities.mapNotNull { entity ->
            val d = parseDateFlexible(entity.startDatum)
            if (d != null && d.year == targetYear) {
                val value = when (goal.metricType) {
                    GoalMetricType.DISTANCE -> entity.distanzKm ?: 0.0
                    GoalMetricType.TIME -> (entity.bewegungszeitSek.toDouble() / 3600.0)
                    GoalMetricType.ELEVATION -> (entity.hoehenmeterM ?: 0).toDouble()
                    GoalMetricType.CALORIES -> (entity.kalorienKcal ?: 0).toDouble()
                    GoalMetricType.WORKOUTS -> 1.0
                }
                d.dayOfYear to value
            } else null
        }.groupBy({ it.first }, { it.second })

        val target = goal.targetValue.coerceAtLeast(1.0)
        val sampleDays = listOf(1, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365)

        for (day in sampleDays) {
            val expectedFraction = getFractionForDay(goal.seasonality, day)

            val actualFraction = if (day <= currentDay) {
                val sumUpToDay = (1..day).sumOf { d -> dailyMap[d]?.sum() ?: 0.0 }
                (sumUpToDay / target).toFloat().coerceIn(0f, 1.25f)
            } else null

            points.add(
                CurvePoint(
                    dayOfYear = day,
                    expectedRatio = expectedFraction.toFloat().coerceIn(0f, 1f),
                    actualRatio = actualFraction
                )
            )
        }

        return points
    }

    private fun getPeriodDates(goal: GoalEntity, referenceDate: LocalDate): Pair<LocalDate, LocalDate> {
        val targetYear = if (goal.year > 2000) goal.year else referenceDate.year
        return when (goal.periodType) {
            GoalPeriodType.WEEKLY -> {
                val start = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                start to end
            }
            GoalPeriodType.MONTHLY -> {
                val start = referenceDate.withDayOfMonth(1)
                val end = referenceDate.with(TemporalAdjusters.lastDayOfMonth())
                start to end
            }
            GoalPeriodType.YEARLY -> {
                val start = LocalDate.of(targetYear, 1, 1)
                val end = LocalDate.of(targetYear, 12, 31)
                start to end
            }
        }
    }

    private fun calculateExpectedValue(
        goal: GoalEntity,
        startDate: LocalDate,
        endDate: LocalDate,
        referenceDate: LocalDate
    ): Pair<Double, Int> {
        val totalDays = (endDate.toEpochDay() - startDate.toEpochDay() + 1).coerceAtLeast(1L).toDouble()
        val elapsedDays = (referenceDate.toEpochDay() - startDate.toEpochDay() + 1)
            .coerceIn(0L, totalDays.toLong()).toDouble()
        val remainingDays = (endDate.toEpochDay() - referenceDate.toEpochDay()).coerceAtLeast(0L).toInt()

        if (goal.periodType != GoalPeriodType.YEARLY) {
            val fraction = (elapsedDays / totalDays).coerceIn(0.0, 1.0)
            return Pair(fraction * goal.targetValue, remainingDays)
        }

        val fraction = getFractionForDay(goal.seasonality, referenceDate.dayOfYear)
        val expected = fraction * goal.targetValue
        return Pair(expected, remainingDays)
    }

    private fun getFractionForDay(seasonality: SeasonalityCurve, dayOfYearInput: Int): Double {
        val day = dayOfYearInput.toDouble().coerceIn(1.0, 365.0)
        return when (seasonality) {
            SeasonalityCurve.LINEAR -> day / 365.0
            SeasonalityCurve.SUMMER_PEAK -> {
                val x = (day - 182.5) / 38.0
                val sigmoid = 1.0 / (1.0 + exp(-x))
                val s0 = 1.0 / (1.0 + exp(-((1.0 - 182.5) / 38.0)))
                val s1 = 1.0 / (1.0 + exp(-((365.0 - 182.5) / 38.0)))
                ((sigmoid - s0) / (s1 - s0)).coerceIn(0.0, 1.0)
            }
            SeasonalityCurve.WINTER_PEAK -> {
                val normalized = (day / 365.0) * 2.0 * Math.PI
                val s = (day / 365.0) - (Math.sin(normalized) / (2.0 * Math.PI)) * 0.48
                s.coerceIn(0.0, 1.0)
            }
        }
    }

    private fun formatMetricValue(value: Double, type: GoalMetricType): String {
        return when (type) {
            GoalMetricType.DISTANCE -> "%.1f km".format(value)
            GoalMetricType.TIME -> "%.1f Std".format(value)
            GoalMetricType.ELEVATION -> "${value.toInt()} m"
            GoalMetricType.CALORIES -> "${value.toInt()} kcal"
            GoalMetricType.WORKOUTS -> "${value.toInt()} x"
        }
    }

    fun parseDateFlexible(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val clean = dateStr.trim().take(10)
        return try {
            LocalDate.parse(clean, isoDateFormatter)
        } catch (_: Exception) {
            try {
                LocalDate.parse(clean, germanDateFormatter)
            } catch (_: Exception) {
                null
            }
        }
    }
}
