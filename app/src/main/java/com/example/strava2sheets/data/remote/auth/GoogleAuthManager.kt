package com.example.strava2sheets.data.remote.auth

import com.example.strava2sheets.data.remote.NetworkClient
import com.example.strava2sheets.data.remote.api.GoogleAuthApiService
import com.example.strava2sheets.data.security.TokenStorage
import retrofit2.HttpException

class GoogleAuthManager(
    private val tokenStorage: TokenStorage? = null,
    private val googleAuthApiService: GoogleAuthApiService? = NetworkClient.googleAuthApiService
) {

    fun saveAccessToken(token: String, expiresInSeconds: Long = 3600) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000)
        tokenStorage?.saveGoogleAccessToken(token, expiresAt)
    }

    fun saveRefreshToken(refreshToken: String) {
        tokenStorage?.saveGoogleRefreshToken(refreshToken)
    }

    fun getRefreshToken(): String? = tokenStorage?.getGoogleRefreshToken()

    fun saveGoogleCredentials(clientId: String, clientSecret: String) {
        tokenStorage?.saveGoogleCredentials(clientId, clientSecret)
    }

    fun getGoogleClientId(): String? = tokenStorage?.getGoogleClientId()
    fun getGoogleClientSecret(): String? = tokenStorage?.getGoogleClientSecret()

    suspend fun refreshAccessToken(): Result<String> {
        val refreshToken = tokenStorage?.getGoogleRefreshToken()?.trim()
        val clientId = tokenStorage?.getGoogleClientId()?.trim() ?: ""
        val clientSecret = tokenStorage?.getGoogleClientSecret()?.trim() ?: ""

        if (refreshToken.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Google Refresh Token ist in den Einstellungen nicht hinterlegt."))
        }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return Result.failure(IllegalStateException("Google Client ID oder Client Secret fehlt in den Einstellungen."))
        }

        if (googleAuthApiService == null) {
            return Result.failure(IllegalStateException("GoogleAuthApiService nicht initialisiert."))
        }

        return try {
            val response = googleAuthApiService.refreshToken(
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken
            )
            saveAccessToken(response.accessToken, response.expiresIn)
            Result.success(response.accessToken)
        } catch (e: Exception) {
            e.printStackTrace()
            val details = if (e is HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                "HTTP ${e.code()}: $errorBody"
            } else {
                e.message ?: "Unbekannter Fehler"
            }
            Result.failure(IllegalStateException("Google OAuth Refresh fehlgeschlagen ($details)"))
        }
    }

    suspend fun getValidAccessToken(): Result<String> {
        val token = tokenStorage?.getGoogleAccessToken()
        if (token != null && tokenStorage?.isGoogleTokenExpired() == false) {
            return Result.success(token)
        }
        return refreshAccessToken()
    }

    fun saveSpreadsheetId(sheetIdOrUrl: String) {
        val cleanId = extractSpreadsheetId(sheetIdOrUrl)
        tokenStorage?.saveSpreadsheetId(cleanId)
    }

    fun getSpreadsheetId(): String? = tokenStorage?.getSpreadsheetId()

    fun extractSpreadsheetId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("docs.google.com/spreadsheets/d/")) {
            val parts = trimmed.split("/d/")
            if (parts.size > 1) {
                return parts[1].split("/")[0]
            }
        }
        return trimmed
    }
}
