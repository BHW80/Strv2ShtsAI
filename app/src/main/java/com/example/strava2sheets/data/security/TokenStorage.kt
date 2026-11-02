package com.example.strava2sheets.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

open class TokenStorage(context: Context? = null) {

    private val sharedPreferences: SharedPreferences? by lazy {
        context?.let { ctx ->
            try {
                val masterKey = MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    ctx,
                    "strava_sync_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                ctx.getSharedPreferences("strava_sync_fallback_prefs", Context.MODE_PRIVATE)
            }
        }
    }

    // Strava Tokens
    open fun saveStravaTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        sharedPreferences?.edit()
            ?.putString(KEY_STRAVA_ACCESS_TOKEN, accessToken)
            ?.putString(KEY_STRAVA_REFRESH_TOKEN, refreshToken)
            ?.putLong(KEY_STRAVA_EXPIRES_AT, expiresAt)
            ?.apply()
    }

    open fun getStravaAccessToken(): String? = sharedPreferences?.getString(KEY_STRAVA_ACCESS_TOKEN, null)
    open fun getStravaRefreshToken(): String? = sharedPreferences?.getString(KEY_STRAVA_REFRESH_TOKEN, null)
    open fun getStravaTokenExpiresAt(): Long = sharedPreferences?.getLong(KEY_STRAVA_EXPIRES_AT, 0L) ?: 0L

    open fun isStravaTokenExpired(): Boolean {
        val expiresAt = getStravaTokenExpiresAt()
        return System.currentTimeMillis() / 1000 >= (expiresAt - 300)
    }

    open fun clearStravaTokens() {
        sharedPreferences?.edit()
            ?.remove(KEY_STRAVA_ACCESS_TOKEN)
            ?.remove(KEY_STRAVA_REFRESH_TOKEN)
            ?.remove(KEY_STRAVA_EXPIRES_AT)
            ?.apply()
    }

    // Strava Client Credentials
    open fun saveStravaCredentials(clientId: String, clientSecret: String) {
        sharedPreferences?.edit()
            ?.putString(KEY_STRAVA_CLIENT_ID, clientId)
            ?.putString(KEY_STRAVA_CLIENT_SECRET, clientSecret)
            ?.apply()
    }

    open fun getStravaClientId(): String? = sharedPreferences?.getString(KEY_STRAVA_CLIENT_ID, null)
    open fun getStravaClientSecret(): String? = sharedPreferences?.getString(KEY_STRAVA_CLIENT_SECRET, null)

    // Google Tokens & Settings
    open fun saveGoogleAccessToken(token: String, expiresAt: Long = System.currentTimeMillis() + 3600_000) {
        sharedPreferences?.edit()
            ?.putString(KEY_GOOGLE_ACCESS_TOKEN, token)
            ?.putLong(KEY_GOOGLE_EXPIRES_AT, expiresAt)
            ?.apply()
    }

    open fun getGoogleAccessToken(): String? = sharedPreferences?.getString(KEY_GOOGLE_ACCESS_TOKEN, null)

    open fun saveGoogleRefreshToken(refreshToken: String) {
        sharedPreferences?.edit()
            ?.putString(KEY_GOOGLE_REFRESH_TOKEN, refreshToken)
            ?.apply()
    }

    open fun getGoogleRefreshToken(): String? = sharedPreferences?.getString(KEY_GOOGLE_REFRESH_TOKEN, null)

    open fun saveGoogleCredentials(clientId: String, clientSecret: String) {
        sharedPreferences?.edit()
            ?.putString(KEY_GOOGLE_CLIENT_ID, clientId)
            ?.putString(KEY_GOOGLE_CLIENT_SECRET, clientSecret)
            ?.apply()
    }

    open fun getGoogleClientId(): String? = sharedPreferences?.getString(KEY_GOOGLE_CLIENT_ID, null)
    open fun getGoogleClientSecret(): String? = sharedPreferences?.getString(KEY_GOOGLE_CLIENT_SECRET, null)

    open fun isGoogleTokenExpired(): Boolean {
        val expiresAt = sharedPreferences?.getLong(KEY_GOOGLE_EXPIRES_AT, 0L) ?: 0L
        return System.currentTimeMillis() >= (expiresAt - 300_000)
    }

    open fun clearGoogleTokens() {
        sharedPreferences?.edit()
            ?.remove(KEY_GOOGLE_ACCESS_TOKEN)
            ?.remove(KEY_GOOGLE_REFRESH_TOKEN)
            ?.remove(KEY_GOOGLE_EXPIRES_AT)
            ?.apply()
    }

    // Google Spreadsheet ID
    open fun saveSpreadsheetId(sheetId: String) {
        sharedPreferences?.edit()
            ?.putString(KEY_SPREADSHEET_ID, sheetId)
            ?.apply()
    }

    open fun getSpreadsheetId(): String? = sharedPreferences?.getString(KEY_SPREADSHEET_ID, null)

    // Gemini AI API Key
    open fun saveGeminiApiKey(apiKey: String) {
        sharedPreferences?.edit()
            ?.putString(KEY_GEMINI_API_KEY, apiKey)
            ?.apply()
    }

    open fun getGeminiApiKey(): String? = sharedPreferences?.getString(KEY_GEMINI_API_KEY, null)

    open fun clearGeminiApiKey() {
        sharedPreferences?.edit()
            ?.remove(KEY_GEMINI_API_KEY)
            ?.apply()
    }

    open fun hasGeminiApiKey(): Boolean = !getGeminiApiKey().isNullOrBlank()

    open fun clearAll() {
        sharedPreferences?.edit()?.clear()?.apply()
    }

    companion object {
        private const val KEY_STRAVA_ACCESS_TOKEN = "strava_access_token"
        private const val KEY_STRAVA_REFRESH_TOKEN = "strava_refresh_token"
        private const val KEY_STRAVA_EXPIRES_AT = "strava_expires_at"
        private const val KEY_STRAVA_CLIENT_ID = "strava_client_id"
        private const val KEY_STRAVA_CLIENT_SECRET = "strava_client_secret"

        private const val KEY_GOOGLE_ACCESS_TOKEN = "google_access_token"
        private const val KEY_GOOGLE_REFRESH_TOKEN = "google_refresh_token"
        private const val KEY_GOOGLE_CLIENT_ID = "google_client_id"
        private const val KEY_GOOGLE_CLIENT_SECRET = "google_client_secret"
        private const val KEY_GOOGLE_EXPIRES_AT = "google_expires_at"

        private const val KEY_SPREADSHEET_ID = "spreadsheet_id"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }
}
