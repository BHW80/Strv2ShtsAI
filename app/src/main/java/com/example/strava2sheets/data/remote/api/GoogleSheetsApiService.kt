package com.example.strava2sheets.data.remote.api

import com.example.strava2sheets.data.remote.dto.AppendValuesResponse
import com.example.strava2sheets.data.remote.dto.ClearValuesResponse
import com.example.strava2sheets.data.remote.dto.UpdateValuesResponse
import com.example.strava2sheets.data.remote.dto.ValueRange
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleSheetsApiService {

    @GET("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun getValues(
        @Header("Authorization") authorization: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String = "A1:V1"
    ): ValueRange

    @PUT("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun updateValues(
        @Header("Authorization") authorization: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String = "A1:V1",
        @Query("valueInputOption") valueInputOption: String = "RAW",
        @Body body: ValueRange
    ): UpdateValuesResponse

    @POST("v4/spreadsheets/{spreadsheetId}/values/{range}:append")
    suspend fun appendValues(
        @Header("Authorization") authorization: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String = "A1",
        @Query("valueInputOption") valueInputOption: String = "RAW",
        @Query("insertDataOption") insertDataOption: String = "INSERT_ROWS",
        @Body body: ValueRange
    ): AppendValuesResponse

    @POST("v4/spreadsheets/{spreadsheetId}/values/{range}:clear")
    suspend fun clearValues(
        @Header("Authorization") authorization: String,
        @Path("spreadsheetId") spreadsheetId: String,
        @Path("range") range: String,
        @Body body: Map<String, String> = emptyMap()
    ): ClearValuesResponse
}
