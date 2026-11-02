package com.example.strava2sheets.data.remote

import com.example.strava2sheets.data.remote.api.GoogleAuthApiService
import com.example.strava2sheets.data.remote.api.GoogleSheetsApiService
import com.example.strava2sheets.data.remote.api.StravaApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkClient {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val contentType = "application/json".toMediaType()

    val stravaApiService: StravaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.strava.com/api/v3/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(StravaApiService::class.java)
    }

    val googleSheetsApiService: GoogleSheetsApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://sheets.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GoogleSheetsApiService::class.java)
    }

    val googleAuthApiService: GoogleAuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://oauth2.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GoogleAuthApiService::class.java)
    }
}
