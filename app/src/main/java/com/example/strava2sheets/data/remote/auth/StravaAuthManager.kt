package com.example.strava2sheets.data.remote.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.strava2sheets.data.remote.api.StravaApiService
import com.example.strava2sheets.data.remote.dto.StravaTokenResponse
import com.example.strava2sheets.data.security.TokenStorage

class StravaAuthManager(
    private val tokenStorage: TokenStorage,
    private val apiService: StravaApiService
) {

    fun getAuthorizationIntent(clientId: String): Intent {
        val redirectUri = REDIRECT_URI_HTTPS
        val authUrl = Uri.parse("https://www.strava.com/oauth/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:read_all,profile:read_all")
            .build()

        return Intent(Intent.ACTION_VIEW, authUrl)
    }

    fun extractAuthorizationCode(uri: Uri?): String? {
        if (uri == null) return null
        val isHttpsRedirect = uri.scheme == "https" && uri.host == "weltler.at" && uri.path?.startsWith("/app-callback") == true
        val isCustomScheme = uri.scheme == "strava-sync" && uri.host == "redirect"
        if (isHttpsRedirect || isCustomScheme) {
            return uri.getQueryParameter("code")
        }
        return null
    }

    suspend fun handleAuthCode(
        clientId: String,
        clientSecret: String,
        code: String
    ): Result<StravaTokenResponse> {
        return try {
            val response = apiService.exchangeToken(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code
            )
            tokenStorage.saveStravaCredentials(clientId, clientSecret)
            tokenStorage.saveStravaTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresAt = response.expiresAt
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getValidAccessToken(): String? {
        val accessToken = tokenStorage.getStravaAccessToken() ?: return null
        val expiresAt = tokenStorage.getStravaTokenExpiresAt()
        val currentTimeSeconds = System.currentTimeMillis() / 1000

        // Wenn Token noch mindestens 60 Sekunden gültig ist
        if (expiresAt - currentTimeSeconds > 60) {
            return accessToken
        }

        val refreshToken = tokenStorage.getStravaRefreshToken() ?: return null
        val clientId = tokenStorage.getStravaClientId() ?: return null
        val clientSecret = tokenStorage.getStravaClientSecret() ?: return null

        return try {
            val response = apiService.refreshToken(
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken
            )
            tokenStorage.saveStravaTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresAt = response.expiresAt
            )
            response.accessToken
        } catch (e: Exception) {
            null
        }
    }

    fun clearTokens() {
        tokenStorage.clearStravaTokens()
    }

    companion object {
        const val REDIRECT_URI_HTTPS = "https://weltler.at/app-callback"
    }
}
