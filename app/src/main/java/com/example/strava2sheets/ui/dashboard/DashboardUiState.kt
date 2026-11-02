package com.example.strava2sheets.ui.dashboard

import com.example.strava2sheets.data.local.ActivityEntity
import com.example.strava2sheets.data.local.AthleteProfileEntity
import com.example.strava2sheets.data.local.GoalEntity
import com.example.strava2sheets.data.repository.IntegrityReport

data class DashboardUiState(
    val isStravaConnected: Boolean = false,
    val isGoogleConnected: Boolean = false,
    val spreadsheetId: String = "",
    val stravaClientId: String = "",
    val stravaClientSecret: String = "",
    val googleAccessTokenInput: String = "",
    val googleRefreshTokenInput: String = "",
    val googleClientIdInput: String = "",
    val googleClientSecretInput: String = "",
    val pendingCount: Int = 0,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncStatusMessage: String = "",
    val isCheckingIntegrity: Boolean = false,
    val isRestoringData: Boolean = false,
    val integrityStatusMessage: String = "",
    val integrityReport: IntegrityReport? = null,
    val missingActivities: List<ActivityEntity> = emptyList(),
    val activities: List<ActivityEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val latestAthleteProfile: AthleteProfileEntity? = null,
    val athleteProfilesHistory: List<AthleteProfileEntity> = emptyList(),
    val isFetchingAthlete: Boolean = false,
    val athleteFetchMessage: String = "",
    val isLoadingPreview: Boolean = false,
    val previewData: List<List<String>> = emptyList(),
    val previewErrorMessage: String = ""
)
