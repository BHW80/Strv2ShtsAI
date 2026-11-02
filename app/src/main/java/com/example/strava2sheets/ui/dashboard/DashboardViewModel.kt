package com.example.strava2sheets.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.strava2sheets.data.local.ActivityDao
import com.example.strava2sheets.data.local.AthleteProfileDao
import com.example.strava2sheets.data.local.AthleteProfileEntity
import com.example.strava2sheets.data.local.GoalDao
import com.example.strava2sheets.data.local.GoalEntity
import com.example.strava2sheets.data.local.SyncStatus
import com.example.strava2sheets.data.remote.NetworkClient
import com.example.strava2sheets.data.remote.auth.GoogleAuthManager
import com.example.strava2sheets.data.remote.auth.StravaAuthManager
import com.example.strava2sheets.data.repository.ActivityRepository
import com.example.strava2sheets.data.repository.AthleteRepository
import com.example.strava2sheets.data.security.TokenStorage
import com.example.strava2sheets.data.worker.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val tokenStorage: TokenStorage,
    private val activityDao: ActivityDao,
    private val goalDao: GoalDao? = null,
    private val athleteProfileDao: AthleteProfileDao? = null,
    private val stravaAuthManager: StravaAuthManager,
    private val googleAuthManager: GoogleAuthManager,
    private val syncManager: SyncManager,
    private val activityRepository: ActivityRepository = ActivityRepository(
        activityDao = activityDao,
        stravaApiService = NetworkClient.stravaApiService,
        googleSheetsApiService = NetworkClient.googleSheetsApiService,
        stravaAuthManager = stravaAuthManager,
        googleAuthManager = googleAuthManager
    ),
    private val athleteRepository: AthleteRepository? = athleteProfileDao?.let {
        AthleteRepository(
            athleteProfileDao = it,
            stravaApiService = NetworkClient.stravaApiService,
            stravaAuthManager = stravaAuthManager
        )
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        observeActivities()
        observeGoals()
        observeAthleteProfiles()
        observeWorkManager()
    }

    fun loadInitialState() {
        viewModelScope.launch {
            val hasStravaToken = tokenStorage.getStravaAccessToken() != null
            val hasGoogleToken = tokenStorage.getGoogleAccessToken() != null || !tokenStorage.getGoogleRefreshToken().isNullOrBlank()
            val sheetId = tokenStorage.getSpreadsheetId() ?: ""
            val clientId = tokenStorage.getStravaClientId() ?: ""
            val clientSecret = tokenStorage.getStravaClientSecret() ?: ""
            val googleToken = tokenStorage.getGoogleAccessToken() ?: ""
            val googleRefreshToken = tokenStorage.getGoogleRefreshToken() ?: ""
            val googleClientId = tokenStorage.getGoogleClientId() ?: ""
            val googleClientSecret = tokenStorage.getGoogleClientSecret() ?: ""

            val pending = activityDao.getCountByStatus(SyncStatus.PENDING)
            val synced = activityDao.getCountByStatus(SyncStatus.SYNCED)
            val failed = activityDao.getCountByStatus(SyncStatus.FAILED)

            _uiState.update {
                it.copy(
                    isStravaConnected = hasStravaToken,
                    isGoogleConnected = hasGoogleToken,
                    spreadsheetId = sheetId,
                    stravaClientId = clientId,
                    stravaClientSecret = clientSecret,
                    googleAccessTokenInput = googleToken,
                    googleRefreshTokenInput = googleRefreshToken,
                    googleClientIdInput = googleClientId,
                    googleClientSecretInput = googleClientSecret,
                    pendingCount = pending,
                    syncedCount = synced,
                    failedCount = failed
                )
            }

            if (sheetId.isNotBlank() && hasGoogleToken) {
                loadSheetPreview()
            }
        }
    }

    private fun observeActivities() {
        viewModelScope.launch {
            activityRepository.allActivities.collect { list ->
                val pending = list.count { it.syncStatus == SyncStatus.PENDING }
                val synced = list.count { it.syncStatus == SyncStatus.SYNCED }
                val failed = list.count { it.syncStatus == SyncStatus.FAILED }

                _uiState.update {
                    it.copy(
                        activities = list,
                        pendingCount = pending,
                        syncedCount = synced,
                        failedCount = failed
                    )
                }
            }
        }
    }

    private fun observeGoals() {
        if (goalDao == null) return
        viewModelScope.launch {
            goalDao.getAllGoalsFlow().collect { goalsList ->
                _uiState.update { it.copy(goals = goalsList) }
            }
        }
    }

    private fun observeAthleteProfiles() {
        if (athleteProfileDao == null) return
        viewModelScope.launch {
            athleteProfileDao.getLatestProfileFlow().collect { latest ->
                _uiState.update { it.copy(latestAthleteProfile = latest) }
            }
        }
        viewModelScope.launch {
            athleteProfileDao.getAllProfilesHistoryFlow().collect { history ->
                _uiState.update { it.copy(athleteProfilesHistory = history) }
            }
        }
    }

    fun fetchAthleteProfile() {
        if (athleteRepository == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingAthlete = true, athleteFetchMessage = "Lade Nutzerdaten von Strava...") }
            val result = athleteRepository.fetchAndSaveAthleteProfile()
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                val details = buildString {
                    append("Erfolgreich aktualisiert!")
                    profile.weightKg?.let { append(" • $it kg") }
                    profile.ftpWatts?.let { append(" • $it Watt") }
                    profile.calculatedWPerKg?.let { append(" • $it W/kg") }
                }
                _uiState.update {
                    it.copy(
                        isFetchingAthlete = false,
                        latestAthleteProfile = profile,
                        athleteFetchMessage = details
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isFetchingAthlete = false,
                        athleteFetchMessage = "Fehler: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun addGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalDao?.insertGoal(goal)
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalDao?.insertGoal(goal)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalDao?.deleteGoal(goal)
        }
    }

    private fun observeWorkManager() {
        viewModelScope.launch {
            syncManager.getOneTimeSyncWorkInfoFlow().collect { workInfos ->
                if (workInfos.isNotEmpty()) {
                    val currentWork = workInfos.first()
                    val isRunning = !currentWork.state.isFinished
                    val statusText = when (currentWork.state) {
                        androidx.work.WorkInfo.State.RUNNING -> "Synchronisierung läuft..."
                        androidx.work.WorkInfo.State.SUCCEEDED -> "Synchronisierung erfolgreich abgeschlossen!"
                        androidx.work.WorkInfo.State.FAILED -> "Synchronisierung fehlgeschlagen."
                        androidx.work.WorkInfo.State.ENQUEUED -> "In der Warteschlange..."
                        else -> ""
                    }
                    _uiState.update { it.copy(isSyncing = isRunning, syncStatusMessage = statusText) }
                }
            }
        }
    }

    fun startStravaAuth(context: Context, clientIdInput: String, clientSecretInput: String) {
        val clientId = clientIdInput.trim()
        val clientSecret = clientSecretInput.trim()

        if (clientId.isBlank() || clientSecret.isBlank()) {
            _uiState.update { it.copy(syncStatusMessage = "Bitte Client ID und Client Secret angeben") }
            return
        }

        tokenStorage.saveStravaCredentials(clientId, clientSecret)
        _uiState.update {
            it.copy(
                stravaClientId = clientId,
                stravaClientSecret = clientSecret
            )
        }

        val authIntent = stravaAuthManager.getAuthorizationIntent(clientId)
        context.startActivity(authIntent)
    }

    fun handleStravaRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")

        if (error != null) {
            _uiState.update { it.copy(syncStatusMessage = "Strava Autorisierung abgebrochen: $error") }
            return
        }

        if (code != null) {
            viewModelScope.launch {
                val clientId = tokenStorage.getStravaClientId() ?: ""
                val clientSecret = tokenStorage.getStravaClientSecret() ?: ""
                val result = stravaAuthManager.handleAuthCode(clientId, clientSecret, code)
                val success = result.isSuccess
                if (success) {
                    _uiState.update {
                        it.copy(
                            isStravaConnected = true,
                            syncStatusMessage = "Strava erfolgreich verbunden!"
                        )
                    }
                    fetchAthleteProfile()
                } else {
                    _uiState.update {
                        it.copy(syncStatusMessage = "Fehler beim Abrufen des Strava Tokens")
                    }
                }
            }
        }
    }

    fun disconnectStrava() {
        viewModelScope.launch {
            stravaAuthManager.clearTokens()
            _uiState.update {
                it.copy(
                    isStravaConnected = false,
                    syncStatusMessage = "Strava getrennt"
                )
            }
        }
    }

    fun saveSpreadsheetId(sheetId: String) {
        val cleanId = sheetId.trim()
        tokenStorage.saveSpreadsheetId(cleanId)
        _uiState.update { it.copy(spreadsheetId = cleanId) }
        if (cleanId.isNotBlank() && _uiState.value.isGoogleConnected) {
            loadSheetPreview()
        }
    }

    fun saveGoogleCredentials(accessToken: String, refreshToken: String = "", clientId: String = "", clientSecret: String = "") {
        val cleanToken = accessToken.trim()
        val cleanRefresh = refreshToken.trim()
        val cleanClientId = clientId.trim()
        val cleanClientSecret = clientSecret.trim()

        if (cleanToken.isNotBlank()) {
            tokenStorage.saveGoogleAccessToken(cleanToken)
        }
        if (cleanRefresh.isNotBlank()) {
            tokenStorage.saveGoogleRefreshToken(cleanRefresh)
        }
        if (cleanClientId.isNotBlank() && cleanClientSecret.isNotBlank()) {
            tokenStorage.saveGoogleCredentials(cleanClientId, cleanClientSecret)
        }

        val isConnected = cleanToken.isNotBlank() || cleanRefresh.isNotBlank()
        _uiState.update {
            it.copy(
                isGoogleConnected = isConnected,
                googleAccessTokenInput = cleanToken,
                googleRefreshTokenInput = cleanRefresh,
                googleClientIdInput = cleanClientId,
                googleClientSecretInput = cleanClientSecret,
                syncStatusMessage = if (isConnected) "Google Anmeldedaten gespeichert!" else "Token gelöscht"
            )
        }

        if (isConnected && _uiState.value.spreadsheetId.isNotBlank()) {
            loadSheetPreview()
        }
    }

    fun disconnectGoogle() {
        tokenStorage.clearGoogleTokens()
        _uiState.update {
            it.copy(
                isGoogleConnected = false,
                googleAccessTokenInput = "",
                googleRefreshTokenInput = "",
                syncStatusMessage = "Google getrennt",
                previewData = emptyList()
            )
        }
    }

    fun startSync() {
        syncManager.startOneTimeSync()
    }

    fun setPeriodicSyncEnabled(enabled: Boolean) {
        if (enabled) {
            syncManager.schedulePeriodicSync()
        }
    }

    fun reFetchAllFromStrava() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncStatusMessage = "Lade alle Aktivitäten von Strava neu...") }
            val result = activityRepository.fetchActivitiesFromStrava(reFetchAll = true)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncStatusMessage = "Strava-Import abgeschlossen! $count Aktivitäten aktualisiert."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncStatusMessage = "Strava-Import fehlgeschlagen: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun checkDataIntegrity() {
        val sheetId = _uiState.value.spreadsheetId
        if (sheetId.isBlank()) {
            _uiState.update { it.copy(integrityStatusMessage = "Fehler: Bitte eine Google Sheet ID angeben!") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingIntegrity = true,
                    integrityStatusMessage = "Prüfe Google Sheet auf Dubletten und Datenunterschiede..."
                )
            }

            val result = activityRepository.checkDataIntegrity(sheetId)
            if (result.isSuccess) {
                val report = result.getOrThrow()
                val message = buildString {
                    if (!report.hasIssues) {
                        append("Tabelle ist perfekt synchron: Keine Dubletten, keine fehlenden Zeilen.")
                    } else {
                        val parts = mutableListOf<String>()
                        if (report.duplicateIdsInSheet.isNotEmpty()) {
                            parts.add("${report.duplicateIdsInSheet.size} Dubletten-IDs")
                        }
                        if (report.missingInSheet.isNotEmpty()) {
                            parts.add("${report.missingInSheet.size} fehlende Zeilen")
                        }
                        if (report.outdatedInSheet.isNotEmpty()) {
                            parts.add("${report.outdatedInSheet.size} veraltete Werte (z.B. Kadenz/Kalorien)")
                        }
                        append("Abweichungen gefunden: " + parts.joinToString(", "))
                    }
                }

                _uiState.update {
                    it.copy(
                        isCheckingIntegrity = false,
                        integrityReport = report,
                        missingActivities = report.missingInSheet,
                        integrityStatusMessage = message
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCheckingIntegrity = false,
                        integrityStatusMessage = "Fehler bei Prüfung: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun syncAllWithoutDuplicates() {
        val sheetId = _uiState.value.spreadsheetId
        if (sheetId.isBlank()) {
            _uiState.update { it.copy(integrityStatusMessage = "Fehler: Bitte eine Google Sheet ID angeben!") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRestoringData = true,
                    integrityStatusMessage = "Bereinige Tabelle und aktualisiere alle Daten dublettenfrei..."
                )
            }

            val result = activityRepository.syncAllActivitiesWithoutDuplicates(sheetId)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _uiState.update {
                    it.copy(
                        isRestoringData = false,
                        integrityReport = null,
                        missingActivities = emptyList(),
                        integrityStatusMessage = "Erfolg: Tabelle komplett bereinigt! $count Aktivitäten ohne Dubletten synchron."
                    )
                }
                loadSheetPreview()
            } else {
                _uiState.update {
                    it.copy(
                        isRestoringData = false,
                        integrityStatusMessage = "Fehler beim Bereinigen: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun restoreMissingActivities() {
        val missing = _uiState.value.missingActivities
        val sheetId = _uiState.value.spreadsheetId
        if (missing.isEmpty() || sheetId.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRestoringData = true,
                    integrityStatusMessage = "Synchronisiere ${missing.size} fehlende Zeilen..."
                )
            }

            val result = activityRepository.restoreMissingActivities(sheetId, missing)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _uiState.update {
                    it.copy(
                        isRestoringData = false,
                        missingActivities = emptyList(),
                        integrityStatusMessage = "Erfolg: $count Aktivitäten nachgetragen!"
                    )
                }
                loadSheetPreview()
            } else {
                _uiState.update {
                    it.copy(
                        isRestoringData = false,
                        integrityStatusMessage = "Fehler beim Nachtragen: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun loadSheetPreview() {
        val sheetId = _uiState.value.spreadsheetId
        if (sheetId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPreview = true) }
            val result = activityRepository.fetchSheetPreviewData(sheetId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoadingPreview = false,
                        previewData = result.getOrDefault(emptyList())
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingPreview = false,
                        syncStatusMessage = "Konnte Vorschau nicht laden: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }
}
