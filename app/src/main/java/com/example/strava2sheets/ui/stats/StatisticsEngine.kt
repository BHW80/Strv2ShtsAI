package com.example.strava2sheets.ui.stats

import com.example.strava2sheets.core.util.DataFormatter
import com.example.strava2sheets.data.local.ActivityEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class StatsTimeRange(val label: String) {
    THIS_WEEK("Diese Woche"),
    THIS_MONTH("Dieser Monat"),
    LAST_30_DAYS("Letzte 30 Tage"),
    BY_YEAR("Nach Jahr"),
    ALL_TIME("Gesamt")
}

enum class YearSubView(val label: String) {
    OVERVIEW("Übersicht"),
    MONTHLY("Monatsübersicht"),
    WEEKLY("Wochenübersicht"),
    SPORTS("Sportarten"),
    PROGRESSION("Progression (YTD)")
}

data class TrendValue(
    val formattedCurrent: String,
    val percentChange: Double?, // e.g. +15.2 or -8.0
    val isPositiveGood: Boolean = true
)

data class PeriodSummary(
    val totalTimeFormatted: String,
    val totalTimeSeconds: Long,
    val totalDistanceKm: Double,
    val totalCalories: Int,
    val totalElevationM: Int,
    val workoutCount: Int,
    val avgHeartRate: Int?,
    val avgWatts: Int?,
    val avgCadence: Int?,
    val maxCadence: Int?,
    val maxWatts: Int?,
    val indoorPercentage: Int, // 0 to 100
    val outdoorPercentage: Int,
    // Trends vs. Previous Period
    val timeTrend: TrendValue,
    val distanceTrend: TrendValue,
    val caloriesTrend: TrendValue,
    val countTrend: TrendValue
)

data class SportBreakdown(
    val sport: String,
    val count: Int,
    val totalDurationSeconds: Long,
    val totalDurationFormatted: String,
    val percentageOfTime: Float, // 0.0 to 1.0
    val totalDistanceKm: Double,
    val avgCadenceOrSpm: Int?,
    val maxCadenceOrSpm: Int?
)

data class PersonalRecordItem(
    val title: String,
    val value: String,
    val activityTitle: String,
    val activityDate: String,
    val sport: String
)

data class PivotItem(
    val periodLabel: String,
    val count: Int,
    val distanceKm: Double,
    val elevationM: Int,
    val timeSeconds: Long,
    val timeFormatted: String,
    val caloriesKcal: Int
)

data class ProgressionMetric(
    val label: String,
    val currentYearValue: String,
    val selectedYearValue: String,
    val diffPercent: Double?
)

data class YtdProgressionComparison(
    val currentYear: Int,
    val selectedYear: Int,
    val asOfDateFormatted: String,
    val metrics: List<ProgressionMetric>
)

data class StatisticsUiState(
    val selectedTimeRange: StatsTimeRange = StatsTimeRange.THIS_MONTH,
    val selectedYear: Int? = null,
    val availableYears: List<Int> = emptyList(),
    val selectedYearSubView: YearSubView = YearSubView.OVERVIEW,
    val selectedSport: String? = null, // null = All sports
    val availableSports: List<String> = emptyList(),
    val summary: PeriodSummary? = null,
    val sportBreakdowns: List<SportBreakdown> = emptyList(),
    val personalRecords: List<PersonalRecordItem> = emptyList(),
    // Pivot Views
    val monthlyPivots: List<PivotItem> = emptyList(),
    val weeklyPivots: List<PivotItem> = emptyList(),
    val pivotTotal: PivotItem? = null,
    val ytdProgression: YtdProgressionComparison? = null
)

object StatisticsEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val germanMonthNames = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    )

    fun calculate(
        activities: List<ActivityEntity>,
        timeRange: StatsTimeRange,
        selectedYear: Int? = null,
        selectedYearSubView: YearSubView = YearSubView.OVERVIEW,
        selectedSport: String? = null,
        referenceDate: LocalDate = LocalDate.now()
    ): StatisticsUiState {
        val allAvailableSports = activities.map { it.sportart }.distinct().sorted()

        val allAvailableYears = activities.mapNotNull {
            try {
                LocalDate.parse(it.startDatum.trim().take(10), dateFormatter).year
            } catch (_: Exception) {
                null
            }
        }.distinct().sortedDescending()

        val activeYear = selectedYear ?: allAvailableYears.firstOrNull() ?: referenceDate.year

        // 1. Determine Date Range Boundaries
        val (startDate, endDate, prevStartDate, prevEndDate) = when (timeRange) {
            StatsTimeRange.THIS_WEEK -> {
                val start = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val prevStart = start.minusWeeks(1)
                val prevEnd = end.minusWeeks(1)
                Quad(start, end, prevStart, prevEnd)
            }
            StatsTimeRange.THIS_MONTH -> {
                val start = referenceDate.withDayOfMonth(1)
                val end = referenceDate.with(TemporalAdjusters.lastDayOfMonth())
                val prevStart = start.minusMonths(1)
                val prevEnd = prevStart.with(TemporalAdjusters.lastDayOfMonth())
                Quad(start, end, prevStart, prevEnd)
            }
            StatsTimeRange.LAST_30_DAYS -> {
                val end = referenceDate
                val start = referenceDate.minusDays(30)
                val prevEnd = start.minusDays(1)
                val prevStart = prevEnd.minusDays(30)
                Quad(start, end, prevStart, prevEnd)
            }
            StatsTimeRange.BY_YEAR -> {
                val start = LocalDate.of(activeYear, 1, 1)
                val end = LocalDate.of(activeYear, 12, 31)
                val prevStart = LocalDate.of(activeYear - 1, 1, 1)
                val prevEnd = LocalDate.of(activeYear - 1, 12, 31)
                Quad(start, end, prevStart, prevEnd)
            }
            StatsTimeRange.ALL_TIME -> {
                Quad(LocalDate.MIN, LocalDate.MAX, LocalDate.MIN, LocalDate.MIN)
            }
        }

        // 2. Filter Activities for Current & Previous Period
        val currentActivities = activities.filter { entity ->
            val d = parseDateSafe(entity.startDatum)
            val sportMatches = selectedSport == null || entity.sportart.equals(selectedSport, ignoreCase = true)
            sportMatches && d != null && !d.isBefore(startDate) && !d.isAfter(endDate)
        }

        val previousActivities = if (timeRange == StatsTimeRange.ALL_TIME) emptyList() else {
            activities.filter { entity ->
                val d = parseDateSafe(entity.startDatum)
                val sportMatches = selectedSport == null || entity.sportart.equals(selectedSport, ignoreCase = true)
                sportMatches && d != null && !d.isBefore(prevStartDate) && !d.isAfter(prevEndDate)
            }
        }

        // 3. Compute Summary KPIs
        val summary = if (currentActivities.isEmpty()) null else {
            val totalTimeSec = currentActivities.sumOf { it.bewegungszeitSek }
            val totalDistKm = currentActivities.sumOf { it.distanzKm ?: 0.0 }
            val totalCal = currentActivities.sumOf { it.kalorienKcal ?: 0 }
            val totalElevM = currentActivities.sumOf { it.hoehenmeterM ?: 0 }
            val count = currentActivities.size

            val hrList = currentActivities.mapNotNull { it.avgHF }.filter { it > 0 }
            val avgHr = if (hrList.isNotEmpty()) hrList.average().toInt() else null

            val wattsList = currentActivities.mapNotNull { it.avgLeistungWatt }.filter { it > 0 }
            val avgW = if (wattsList.isNotEmpty()) wattsList.average().toInt() else null
            val maxW = currentActivities.mapNotNull { it.npleistungWatt ?: it.avgLeistungWatt }.maxOrNull()

            val cadList = currentActivities.mapNotNull { it.avgCadence }.filter { it > 0 }
            val avgCad = if (cadList.isNotEmpty()) cadList.average().toInt() else null
            val maxCad = currentActivities.mapNotNull { it.maxCadence }.filter { it > 0 }.maxOrNull()

            val indoorCount = currentActivities.count { it.istIndoor }
            val indoorPct = if (count > 0) ((indoorCount.toDouble() / count) * 100).toInt() else 0
            val outdoorPct = 100 - indoorPct

            // Previous Period Metrics for Trends
            val prevTimeSec = previousActivities.sumOf { it.bewegungszeitSek }
            val prevDistKm = previousActivities.sumOf { it.distanzKm ?: 0.0 }
            val prevCal = previousActivities.sumOf { it.kalorienKcal ?: 0 }
            val prevCount = previousActivities.size

            PeriodSummary(
                totalTimeFormatted = DataFormatter.formatSecondsToHms(totalTimeSec),
                totalTimeSeconds = totalTimeSec,
                totalDistanceKm = totalDistKm,
                totalCalories = totalCal,
                totalElevationM = totalElevM,
                workoutCount = count,
                avgHeartRate = avgHr,
                avgWatts = avgW,
                avgCadence = avgCad,
                maxCadence = maxCad,
                maxWatts = maxW,
                indoorPercentage = indoorPct,
                outdoorPercentage = outdoorPct,
                timeTrend = TrendValue(
                    formattedCurrent = DataFormatter.formatSecondsToHms(totalTimeSec),
                    percentChange = calcPercentChange(prevTimeSec.toDouble(), totalTimeSec.toDouble())
                ),
                distanceTrend = TrendValue(
                    formattedCurrent = "%.1f km".format(totalDistKm),
                    percentChange = calcPercentChange(prevDistKm, totalDistKm)
                ),
                caloriesTrend = TrendValue(
                    formattedCurrent = "$totalCal kcal",
                    percentChange = calcPercentChange(prevCal.toDouble(), totalCal.toDouble())
                ),
                countTrend = TrendValue(
                    formattedCurrent = "$count Workouts",
                    percentChange = calcPercentChange(prevCount.toDouble(), count.toDouble())
                )
            )
        }

        // 4. Sport Breakdown
        val sportBreakdowns = currentActivities.groupBy { it.sportart }
            .map { (sport, list) ->
                val durationSec = list.sumOf { it.bewegungszeitSek }
                val totalSec = currentActivities.sumOf { it.bewegungszeitSek }.coerceAtLeast(1L)
                val cads = list.mapNotNull { it.avgCadence }.filter { it > 0 }
                val maxCads = list.mapNotNull { it.maxCadence }.filter { it > 0 }

                SportBreakdown(
                    sport = sport,
                    count = list.size,
                    totalDurationSeconds = durationSec,
                    totalDurationFormatted = DataFormatter.formatSecondsToHms(durationSec),
                    percentageOfTime = (durationSec.toFloat() / totalSec).coerceIn(0f, 1f),
                    totalDistanceKm = list.sumOf { it.distanzKm ?: 0.0 },
                    avgCadenceOrSpm = if (cads.isNotEmpty()) cads.average().toInt() else null,
                    maxCadenceOrSpm = maxCads.maxOrNull()
                )
            }.sortedByDescending { it.totalDurationSeconds }

        // 5. Personal Bests
        val personalRecords = buildPersonalRecords(currentActivities)

        // 6. Year-Specific Pivot Calculations (Monthly, Weekly, YTD Progression)
        val (monthlyPivots, weeklyPivots, pivotTotal) = if (timeRange == StatsTimeRange.BY_YEAR) {
            calculatePivotsForYear(currentActivities)
        } else {
            Triple(emptyList(), emptyList(), null)
        }

        val ytdProgression = if (timeRange == StatsTimeRange.BY_YEAR) {
            calculateYtdProgression(
                activities = activities,
                selectedYear = activeYear,
                currentYear = referenceDate.year,
                referenceDate = referenceDate,
                selectedSport = selectedSport
            )
        } else null

        return StatisticsUiState(
            selectedTimeRange = timeRange,
            selectedYear = activeYear,
            availableYears = allAvailableYears,
            selectedYearSubView = selectedYearSubView,
            selectedSport = selectedSport,
            availableSports = allAvailableSports,
            summary = summary,
            sportBreakdowns = sportBreakdowns,
            personalRecords = personalRecords,
            monthlyPivots = monthlyPivots,
            weeklyPivots = weeklyPivots,
            pivotTotal = pivotTotal,
            ytdProgression = ytdProgression
        )
    }

    private fun calculatePivotsForYear(activities: List<ActivityEntity>): Triple<List<PivotItem>, List<PivotItem>, PivotItem?> {
        if (activities.isEmpty()) return Triple(emptyList(), emptyList(), null)

        // Monats-Pivot (1-12)
        val byMonth = activities.groupBy {
            parseDateSafe(it.startDatum)?.monthValue ?: 1
        }

        val monthly = (1..12).map { monthNum ->
            val list = byMonth[monthNum] ?: emptyList()
            val timeSec = list.sumOf { it.bewegungszeitSek }
            PivotItem(
                periodLabel = germanMonthNames[monthNum - 1],
                count = list.size,
                distanceKm = list.sumOf { it.distanzKm ?: 0.0 },
                elevationM = list.sumOf { it.hoehenmeterM ?: 0 },
                timeSeconds = timeSec,
                timeFormatted = DataFormatter.formatSecondsToHms(timeSec),
                caloriesKcal = list.sumOf { it.kalorienKcal ?: 0 }
            )
        }.filter { it.count > 0 || it.distanceKm > 0.0 || it.timeSeconds > 0L }

        // Wochen-Pivot (ISO Kalenderwochen)
        val weekFields = WeekFields.of(Locale.GERMANY)
        val byWeek = activities.groupBy {
            val d = parseDateSafe(it.startDatum)
            if (d != null) d.get(weekFields.weekOfWeekBasedYear()) else 0
        }

        val weekly = byWeek.keys.filter { it > 0 }.sorted().map { kw ->
            val list = byWeek[kw] ?: emptyList()
            val timeSec = list.sumOf { it.bewegungszeitSek }
            PivotItem(
                periodLabel = "KW %02d".format(kw),
                count = list.size,
                distanceKm = list.sumOf { it.distanzKm ?: 0.0 },
                elevationM = list.sumOf { it.hoehenmeterM ?: 0 },
                timeSeconds = timeSec,
                timeFormatted = DataFormatter.formatSecondsToHms(timeSec),
                caloriesKcal = list.sumOf { it.kalorienKcal ?: 0 }
            )
        }

        // Gesamtsumme
        val totalSec = activities.sumOf { it.bewegungszeitSek }
        val total = PivotItem(
            periodLabel = "Gesamt",
            count = activities.size,
            distanceKm = activities.sumOf { it.distanzKm ?: 0.0 },
            elevationM = activities.sumOf { it.hoehenmeterM ?: 0 },
            timeSeconds = totalSec,
            timeFormatted = DataFormatter.formatSecondsToHms(totalSec),
            caloriesKcal = activities.sumOf { it.kalorienKcal ?: 0 }
        )

        return Triple(monthly, weekly, total)
    }

    private fun calculateYtdProgression(
        activities: List<ActivityEntity>,
        selectedYear: Int,
        currentYear: Int,
        referenceDate: LocalDate,
        selectedSport: String?
    ): YtdProgressionComparison {
        val currentDayOfYear = referenceDate.dayOfYear
        val asOfDateFormatted = "%02d.%02d.".format(referenceDate.dayOfMonth, referenceDate.monthValue)

        // Filtern nach YTD (1. Jan bis heute für beide Jahre)
        fun getYtdList(year: Int): List<ActivityEntity> {
            return activities.filter { entity ->
                val d = parseDateSafe(entity.startDatum)
                val sportMatches = selectedSport == null || entity.sportart.equals(selectedSport, ignoreCase = true)
                sportMatches && d != null && d.year == year && d.dayOfYear <= currentDayOfYear
            }
        }

        val currList = getYtdList(currentYear)
        val selList = getYtdList(selectedYear)

        val currDist = currList.sumOf { it.distanzKm ?: 0.0 }
        val selDist = selList.sumOf { it.distanzKm ?: 0.0 }

        val currElev = currList.sumOf { it.hoehenmeterM ?: 0 }
        val selElev = selList.sumOf { it.hoehenmeterM ?: 0 }

        val currTimeSec = currList.sumOf { it.bewegungszeitSek }
        val selTimeSec = selList.sumOf { it.bewegungszeitSek }

        val currCal = currList.sumOf { it.kalorienKcal ?: 0 }
        val selCal = selList.sumOf { it.kalorienKcal ?: 0 }

        val currCount = currList.size
        val selCount = selList.size

        val metrics = listOf(
            ProgressionMetric(
                label = "Distanz",
                currentYearValue = "%.1f km".format(currDist),
                selectedYearValue = "%.1f km".format(selDist),
                diffPercent = calcPercentChange(selDist, currDist)
            ),
            ProgressionMetric(
                label = "Höhenmeter",
                currentYearValue = "$currElev m",
                selectedYearValue = "$selElev m",
                diffPercent = calcPercentChange(selElev.toDouble(), currElev.toDouble())
            ),
            ProgressionMetric(
                label = "Netto-Trainingszeit",
                currentYearValue = DataFormatter.formatSecondsToHms(currTimeSec),
                selectedYearValue = DataFormatter.formatSecondsToHms(selTimeSec),
                diffPercent = calcPercentChange(selTimeSec.toDouble(), currTimeSec.toDouble())
            ),
            ProgressionMetric(
                label = "Kalorien",
                currentYearValue = "$currCal kcal",
                selectedYearValue = "$selCal kcal",
                diffPercent = calcPercentChange(selCal.toDouble(), currCal.toDouble())
            ),
            ProgressionMetric(
                label = "Workouts",
                currentYearValue = "$currCount Einheiten",
                selectedYearValue = "$selCount Einheiten",
                diffPercent = calcPercentChange(selCount.toDouble(), currCount.toDouble())
            )
        )

        return YtdProgressionComparison(
            currentYear = currentYear,
            selectedYear = selectedYear,
            asOfDateFormatted = asOfDateFormatted,
            metrics = metrics
        )
    }

    private fun buildPersonalRecords(activities: List<ActivityEntity>): List<PersonalRecordItem> {
        val records = mutableListOf<PersonalRecordItem>()
        if (activities.isEmpty()) return records

        // 1. Longest Distance
        activities.maxByOrNull { it.distanzKm ?: 0.0 }?.let { best ->
            if ((best.distanzKm ?: 0.0) > 0.0) {
                records.add(
                    PersonalRecordItem(
                        title = "Längste Distanz",
                        value = "%.2f km".format(best.distanzKm),
                        activityTitle = best.titel,
                        activityDate = best.startDatum,
                        sport = best.sportart
                    )
                )
            }
        }

        // 2. Longest Duration
        activities.maxByOrNull { it.bewegungszeitSek }?.let { best ->
            if (best.bewegungszeitSek > 0) {
                records.add(
                    PersonalRecordItem(
                        title = "Längste Dauer",
                        value = best.bewegungszeitFormatted,
                        activityTitle = best.titel,
                        activityDate = best.startDatum,
                        sport = best.sportart
                    )
                )
            }
        }

        // 3. Highest Elevation Gain
        activities.maxByOrNull { it.hoehenmeterM ?: 0 }?.let { best ->
            if ((best.hoehenmeterM ?: 0) > 0) {
                records.add(
                    PersonalRecordItem(
                        title = "Meiste Höhenmeter",
                        value = "${best.hoehenmeterM} m",
                        activityTitle = best.titel,
                        activityDate = best.startDatum,
                        sport = best.sportart
                    )
                )
            }
        }

        // 4. Highest Calories
        activities.maxByOrNull { it.kalorienKcal ?: 0 }?.let { best ->
            if ((best.kalorienKcal ?: 0) > 0) {
                records.add(
                    PersonalRecordItem(
                        title = "Höchster Kalorienverbrauch",
                        value = "${best.kalorienKcal} kcal",
                        activityTitle = best.titel,
                        activityDate = best.startDatum,
                        sport = best.sportart
                    )
                )
            }
        }

        // 5. Max Speed
        activities.maxByOrNull { it.maxGeschwindigkeitKmh ?: 0.0 }?.let { best ->
            if ((best.maxGeschwindigkeitKmh ?: 0.0) > 0.0) {
                records.add(
                    PersonalRecordItem(
                        title = "Max. Geschwindigkeit",
                        value = "%.1f km/h".format(best.maxGeschwindigkeitKmh),
                        activityTitle = best.titel,
                        activityDate = best.startDatum,
                        sport = best.sportart
                    )
                )
            }
        }

        // 6. Max Cadence / Stroke Rate
        activities.maxByOrNull { it.maxCadence ?: 0 }?.let { best ->
            if ((best.maxCadence ?: 0) > 0) {
                records.add(
                    PersonalRecordItem(
                        title = "Peak Kadenz / SPM",
                        value = "${best.maxCadence} rpm/spm",
                        activityTitle = best.titel,
                        activityDate = best.startDatum,
                        sport = best.sportart
                    )
                )
            }
        }

        return records
    }

    private fun parseDateSafe(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr.trim().take(10), dateFormatter)
        } catch (_: Exception) {
            null
        }
    }

    private fun calcPercentChange(prev: Double, current: Double): Double? {
        if (prev <= 0.0) return null
        val diff = ((current - prev) / prev) * 100.0
        return Math.round(diff * 10.0) / 10.0
    }

    private data class Quad(
        val start: LocalDate,
        val end: LocalDate,
        val prevStart: LocalDate,
        val prevEnd: LocalDate
    )
}
