package com.example.strava2sheets.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalPeriodType {
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class GoalMetricType(val label: String, val unit: String) {
    DISTANCE("Distanz", "km"),
    TIME("Netto-Zeit", "Std"),
    ELEVATION("Hm", "m"),
    CALORIES("Kalorien", "kcal"),
    WORKOUTS("Einheiten", "x")
}

enum class SeasonalityCurve(val label: String) {
    LINEAR("Gleichmäßig (Linear)"),
    SUMMER_PEAK("Sommer-Peak (Rennrad, MTB, Outdoor)"),
    WINTER_PEAK("Winter-Peak (Rudern, Ergometer, Indoor)")
}

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sportart: String?, // null = alle Sportarten
    val metricType: GoalMetricType,
    val targetValue: Double,
    val periodType: GoalPeriodType,
    val year: Int = 2026,
    val seasonality: SeasonalityCurve = SeasonalityCurve.LINEAR,
    val createdAt: Long = System.currentTimeMillis()
)
