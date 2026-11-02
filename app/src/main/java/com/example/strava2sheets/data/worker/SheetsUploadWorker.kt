package com.example.strava2sheets.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.strava2sheets.data.local.AppDatabase
import com.example.strava2sheets.data.remote.NetworkClient
import com.example.strava2sheets.data.remote.auth.GoogleAuthManager
import com.example.strava2sheets.data.remote.auth.StravaAuthManager
import com.example.strava2sheets.data.repository.ActivityRepository
import com.example.strava2sheets.data.security.TokenStorage

class SheetsUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tokenStorage = TokenStorage(applicationContext)
        val database = AppDatabase.getInstance(applicationContext)
        val stravaAuthManager = StravaAuthManager(tokenStorage, NetworkClient.stravaApiService)
        val googleAuthManager = GoogleAuthManager(tokenStorage)

        val sheetId = googleAuthManager.getSpreadsheetId()
        if (sheetId.isNullOrBlank()) {
            return Result.failure()
        }

        val repository = ActivityRepository(
            activityDao = database.activityDao(),
            stravaApiService = NetworkClient.stravaApiService,
            googleSheetsApiService = NetworkClient.googleSheetsApiService,
            stravaAuthManager = stravaAuthManager,
            googleAuthManager = googleAuthManager
        )

        val result = repository.uploadPendingActivitiesToGoogleSheets(sheetId)

        return if (result.isSuccess) {
            Result.success()
        } else {
            val exception = result.exceptionOrNull()
            if (exception is IllegalStateException || exception is IllegalArgumentException) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}
