package com.example.strava2sheets.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class SyncManager(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun startOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val fetchWorkRequest = OneTimeWorkRequestBuilder<StravaFetchWorker>()
            .setConstraints(constraints)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<SheetsUploadWorker>()
            .setConstraints(constraints)
            .build()

        workManager
            .beginUniqueWork(
                WORK_NAME_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                fetchWorkRequest
            )
            .then(uploadWorkRequest)
            .enqueue()
    }

    fun schedulePeriodicSync(repeatIntervalHours: Long = 6) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<StravaFetchWorker>(
            repeatIntervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun getOneTimeSyncWorkInfoFlow(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME_ONE_TIME)
    }

    companion object {
        private const val WORK_NAME_ONE_TIME = "Strava2SheetsOneTimeSync"
        private const val WORK_NAME_PERIODIC = "Strava2SheetsPeriodicSync"
    }
}
