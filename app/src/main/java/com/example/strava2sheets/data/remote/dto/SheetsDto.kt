package com.example.strava2sheets.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValueRange(
    @SerialName("range") val range: String? = null,
    @SerialName("majorDimension") val majorDimension: String = "ROWS",
    @SerialName("values") val values: List<List<String>>? = emptyList()
) {
    val rows: List<List<String>>
        get() = values ?: emptyList()
}

@Serializable
data class AppendValuesResponse(
    @SerialName("spreadsheetId") val spreadsheetId: String? = null,
    @SerialName("tableRange") val tableRange: String? = null,
    @SerialName("updates") val updates: UpdateValuesResponse? = null
)

@Serializable
data class UpdateValuesResponse(
    @SerialName("spreadsheetId") val spreadsheetId: String? = null,
    @SerialName("updatedRange") val updatedRange: String? = null,
    @SerialName("updatedRows") val updatedRows: Int? = null,
    @SerialName("updatedColumns") val updatedColumns: Int? = null,
    @SerialName("updatedCells") val updatedCells: Int? = null
)

@Serializable
data class ClearValuesResponse(
    @SerialName("spreadsheetId") val spreadsheetId: String? = null,
    @SerialName("clearedRange") val clearedRange: String? = null
)

@Serializable
data class GoogleTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600
)
