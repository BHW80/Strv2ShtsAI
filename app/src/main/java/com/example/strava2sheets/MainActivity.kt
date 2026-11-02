package com.example.strava2sheets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.strava2sheets.data.local.AppDatabase
import com.example.strava2sheets.data.remote.NetworkClient
import com.example.strava2sheets.data.remote.auth.GoogleAuthManager
import com.example.strava2sheets.data.remote.auth.StravaAuthManager
import com.example.strava2sheets.data.security.TokenStorage
import com.example.strava2sheets.data.worker.SyncManager
import com.example.strava2sheets.ui.dashboard.DashboardViewModel
import com.example.strava2sheets.ui.navigation.MainAppNavigation

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val tokenStorage = TokenStorage(this@MainActivity)
                val db = AppDatabase.getInstance(this@MainActivity)
                val stravaAuthManager = StravaAuthManager(tokenStorage, NetworkClient.stravaApiService)
                val googleAuthManager = GoogleAuthManager(tokenStorage)
                val syncManager = SyncManager(this@MainActivity)

                return DashboardViewModel(
                    tokenStorage = tokenStorage,
                    activityDao = db.activityDao(),
                    goalDao = db.goalDao(),
                    athleteProfileDao = db.athleteProfileDao(),
                    stravaAuthManager = stravaAuthManager,
                    googleAuthManager = googleAuthManager,
                    syncManager = syncManager
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.handleStravaRedirect(intent)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.handleStravaRedirect(intent)
    }
}
