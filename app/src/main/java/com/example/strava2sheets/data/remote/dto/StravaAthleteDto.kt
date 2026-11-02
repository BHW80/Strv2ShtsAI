package com.example.strava2sheets.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StravaAthleteDto(
    @SerialName("id") val id: Long,
    @SerialName("firstname") val firstname: String? = null,
    @SerialName("lastname") val lastname: String? = null,
    @SerialName("profile_medium") val profileMedium: String? = null,
    @SerialName("profile") val profile: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("sex") val sex: String? = null,
    @SerialName("weight") val weight: Double? = null,
    @SerialName("ftp") val ftp: Double? = null // Kann als Double, Float oder Int von Strava geliefert werden
)

@Serializable
data class StravaZoneBucket(
    @SerialName("min") val min: Int = 0,
    @SerialName("max") val max: Int = 0
)

@Serializable
data class StravaHeartRateZones(
    @SerialName("custom_zones") val customZones: Boolean? = false,
    @SerialName("zones") val zones: List<StravaZoneBucket>? = null
)

@Serializable
data class StravaPowerZones(
    @SerialName("zones") val zones: List<StravaZoneBucket>? = null
)

@Serializable
data class StravaAthleteZonesDto(
    @SerialName("heart_rate") val heartRate: StravaHeartRateZones? = null,
    @SerialName("power") val power: StravaPowerZones? = null
)
