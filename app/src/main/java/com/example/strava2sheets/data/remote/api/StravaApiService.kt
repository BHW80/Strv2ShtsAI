package com.example.strava2sheets.data.remote.api

import com.example.strava2sheets.data.remote.dto.StravaActivityDto
import com.example.strava2sheets.data.remote.dto.StravaAthleteDto
import com.example.strava2sheets.data.remote.dto.StravaAthleteZonesDto
import com.example.strava2sheets.data.remote.dto.StravaTokenResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StravaApiService {

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun exchangeToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code"
    ): StravaTokenResponse

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): StravaTokenResponse

    @GET("athlete")
    suspend fun getAuthenticatedAthlete(
        @Header("Authorization") authorization: String
    ): StravaAthleteDto

    @GET("athlete/zones")
    suspend fun getAthleteZones(
        @Header("Authorization") authorization: String
    ): StravaAthleteZonesDto

    @GET("athlete/activities")
    suspend fun getAthleteActivities(
        @Header("Authorization") authorization: String,
        @Query("after") afterEpochSeconds: Long? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 200
    ): List<StravaActivityDto>

    @GET("activities/{id}")
    suspend fun getActivityDetailRaw(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Query("include_all_efforts") includeAllEfforts: Boolean = false
    ): JsonElement

    @GET("activities/{id}/streams")
    suspend fun getActivityStreamsRaw(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Query(value = "keys", encoded = true) keys: String = "time,cadence",
        @Query("key_by_type") keyByType: Boolean = true
    ): JsonElement
}
