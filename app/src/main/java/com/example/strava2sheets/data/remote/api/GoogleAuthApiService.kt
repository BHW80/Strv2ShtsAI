package com.example.strava2sheets.data.remote.api

import com.example.strava2sheets.data.remote.dto.GoogleTokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface GoogleAuthApiService {

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): GoogleTokenResponse
}
