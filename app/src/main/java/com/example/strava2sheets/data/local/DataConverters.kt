package com.example.strava2sheets.data.local

import androidx.room.TypeConverter

class DataConverters {

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return try {
            SyncStatus.valueOf(value)
        } catch (e: Exception) {
            SyncStatus.PENDING
        }
    }

    @TypeConverter
    fun fromGoalPeriodType(type: GoalPeriodType): String {
        return type.name
    }

    @TypeConverter
    fun toGoalPeriodType(value: String): GoalPeriodType {
        return try {
            GoalPeriodType.valueOf(value)
        } catch (e: Exception) {
            GoalPeriodType.YEARLY
        }
    }

    @TypeConverter
    fun fromGoalMetricType(type: GoalMetricType): String {
        return type.name
    }

    @TypeConverter
    fun toGoalMetricType(value: String): GoalMetricType {
        return try {
            GoalMetricType.valueOf(value)
        } catch (e: Exception) {
            GoalMetricType.DISTANCE
        }
    }

    @TypeConverter
    fun fromSeasonalityCurve(curve: SeasonalityCurve): String {
        return curve.name
    }

    @TypeConverter
    fun toSeasonalityCurve(value: String): SeasonalityCurve {
        return try {
            SeasonalityCurve.valueOf(value)
        } catch (e: Exception) {
            SeasonalityCurve.LINEAR
        }
    }
}
