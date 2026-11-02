package com.example.strava2sheets.data.repository

import android.util.Log
import com.example.strava2sheets.data.local.ActivityDao
import com.example.strava2sheets.data.local.ActivityEntity
import com.example.strava2sheets.data.local.SyncStatus
import com.example.strava2sheets.data.remote.api.GoogleSheetsApiService
import com.example.strava2sheets.data.remote.api.StravaApiService
import com.example.strava2sheets.data.remote.auth.GoogleAuthManager
import com.example.strava2sheets.data.remote.auth.StravaAuthManager
import com.example.strava2sheets.data.remote.dto.StravaStreamParser
import com.example.strava2sheets.data.remote.dto.ValueRange
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

data class IntegrityReport(
    val missingInSheet: List<ActivityEntity> = emptyList(),
    val duplicateIdsInSheet: List<Long> = emptyList(),
    val outdatedInSheet: List<ActivityEntity> = emptyList(),
    val totalSheetRows: Int = 0,
    val totalLocalActivities: Int = 0
) {
    val hasIssues: Boolean
        get() = missingInSheet.isNotEmpty() || duplicateIdsInSheet.isNotEmpty() || outdatedInSheet.isNotEmpty()

    val totalIssuesCount: Int
        get() = missingInSheet.size + duplicateIdsInSheet.size + outdatedInSheet.size
}

class ActivityRepository(
    private val activityDao: ActivityDao,
    private val stravaApiService: StravaApiService,
    private val googleSheetsApiService: GoogleSheetsApiService,
    private val stravaAuthManager: StravaAuthManager,
    private val googleAuthManager: GoogleAuthManager
) {

    val allActivities: Flow<List<ActivityEntity>> = activityDao.getAllActivitiesFlow()

    suspend fun getPendingCount(): Int {
        return activityDao.getCountByStatus(SyncStatus.PENDING)
    }

    suspend fun getLatestFetchedTimestamp(): Long? {
        return activityDao.getLatestFetchedTimestamp()
    }

    suspend fun fetchActivitiesFromStrava(reFetchAll: Boolean = false): Result<Int> {
        val accessToken = stravaAuthManager.getValidAccessToken()
            ?: return Result.failure(IllegalStateException("Strava nicht authentifiziert oder Token abgelaufen"))

        return try {
            val afterSeconds = if (reFetchAll) null else getLatestFetchedTimestamp()?.let { it / 1000 }

            var page = 1
            var totalNewInserted = 0
            val perPage = 200

            while (true) {
                val authHeader = "Bearer $accessToken"
                val dtos = stravaApiService.getAthleteActivities(
                    authorization = authHeader,
                    afterEpochSeconds = afterSeconds,
                    page = page,
                    perPage = perPage
                )

                if (dtos.isEmpty()) break

                val entities = dtos.map { it.toEntity() }
                val insertedIds = activityDao.insertOrIgnoreActivities(entities)
                val newCount = insertedIds.count { it != -1L }
                totalNewInserted += newCount

                if (dtos.size < perPage) break
                page++
            }

            // Performance-Optimierung: Detail-Enrichment NUR für Aktivitäten, die von der Strava Summary API
            // keine Kalorien/Kadenz erhalten (primär Rudern: Rowing, VirtualRow, Canoe, Kayak)
            val allLocal = activityDao.getAllActivitiesList()
            val candidateActivities = allLocal.filter {
                isSpecialEnrichmentNeeded(it)
            }.sortedByDescending { it.activityId }
            .take(15) // Batch-Größe für minimale Ladezeiten

            if (candidateActivities.isNotEmpty()) {
                enrichActivityDetails(
                    activitiesToEnrich = candidateActivities,
                    accessToken = accessToken
                )
            }

            Result.success(totalNewInserted)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * Prüft, ob eine Aktivität von Strava's Summary API keine Details mitbringt
     * (insb. Ruderaktivitäten bzgl. Kalorien oder SPM / Max-Kadenz).
     */
    private fun isSpecialEnrichmentNeeded(activity: ActivityEntity): Boolean {
        val isRowingOrPaddling = activity.sportart.contains("Row", ignoreCase = true) ||
                activity.sportart.contains("Canoe", ignoreCase = true) ||
                activity.sportart.contains("Kayak", ignoreCase = true)

        if (isRowingOrPaddling) {
            return activity.kalorienKcal == null || activity.maxCadence == null
        }
        return false
    }

    /**
     * Holt gezielt Detail- und Stream-Werte (z.B. Schlagzahl-Stream / SPM & Kalorien) von Strava.
     */
    suspend fun enrichActivityDetails(
        activitiesToEnrich: List<ActivityEntity>,
        accessToken: String? = null
    ) {
        val token = accessToken ?: stravaAuthManager.getValidAccessToken() ?: return
        val authHeader = "Bearer $token"

        for (activity in activitiesToEnrich) {
            try {
                var updated = activity

                // 1. Detaillierte Aktivität für Kalorien abrufen
                try {
                    val rawDetailJson = stravaApiService.getActivityDetailRaw(authHeader, activity.activityId)
                    val extractedCalories = StravaStreamParser.extractCalories(rawDetailJson)
                    val extractedMaxCadence = StravaStreamParser.extractMaxCadence(rawDetailJson)

                    if (extractedCalories != null && extractedCalories > 0) {
                        updated = updated.copy(
                            kalorienKcal = extractedCalories,
                            energieKj = updated.energieKj ?: extractedCalories
                        )
                    }

                    if (extractedMaxCadence != null && extractedMaxCadence > 0) {
                        updated = updated.copy(maxCadence = extractedMaxCadence)
                    }
                } catch (e: Exception) {
                    Log.w("ActivityRepository", "Failed to fetch activity detail for ${activity.activityId}: ${e.message}")
                }

                // 2. Stream nur abfragen, wenn Max-Kadenz noch fehlt
                if (updated.maxCadence == null && (updated.avgCadence != null && updated.avgCadence > 0)) {
                    try {
                        val rawStreamJson = stravaApiService.getActivityStreamsRaw(
                            authorization = authHeader,
                            id = activity.activityId,
                            keys = "time,cadence",
                            keyByType = true
                        )
                        val maxCadenceFromStream = StravaStreamParser.extractMaxCadence(rawStreamJson)
                        if (maxCadenceFromStream != null && maxCadenceFromStream > 0) {
                            updated = updated.copy(maxCadence = maxCadenceFromStream)
                        }
                    } catch (streamEx: Exception) {
                        Log.w("ActivityRepository", "Failed to fetch cadence stream for ${activity.activityId}: ${streamEx.message}")
                    }
                }

                activityDao.insertOrUpdateActivity(updated)
            } catch (ex: Exception) {
                Log.e("ActivityRepository", "Error enriching activity ${activity.activityId}", ex)
            }
        }
    }

    suspend fun uploadPendingActivitiesToGoogleSheets(sheetIdInput: String): Result<Int> {
        val sheetId = googleAuthManager.extractSpreadsheetId(sheetIdInput)
        if (sheetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Google Spreadsheet ID ist leer"))
        }

        val tokenResult = googleAuthManager.getValidAccessToken()
        val googleToken = tokenResult.getOrElse {
            return Result.failure(it)
        }

        var pendingActivities = activityDao.getActivitiesByStatus(SyncStatus.PENDING, limit = 200)
        if (pendingActivities.isEmpty()) {
            return Result.success(0)
        }

        // Schnelle Vorprüfung: Nur Ruder-Aktivitäten ohne Werte anreichern (max 5)
        val rowingPending = pendingActivities.filter { isSpecialEnrichmentNeeded(it) }
        if (rowingPending.isNotEmpty()) {
            enrichActivityDetails(rowingPending.take(5))
            pendingActivities = activityDao.getActivitiesByStatus(SyncStatus.PENDING, limit = 200)
        }

        val authHeader = "Bearer $googleToken"

        return try {
            ensureSheetHeadersExist(authHeader, sheetId)

            // Deduplizierung: Spalte A abfragen, damit keine Zeilen doppelt angehängt werden
            val existingIdsResponse = googleSheetsApiService.getValues(
                authorization = authHeader,
                spreadsheetId = sheetId,
                range = "A:A"
            )
            val existingIdsInSheet = existingIdsResponse.rows
                .mapNotNull { row -> row.firstOrNull()?.trim()?.toLongOrNull() }
                .toSet()

            val activitiesToUpload = pendingActivities.filter { it.activityId !in existingIdsInSheet }

            if (activitiesToUpload.isEmpty()) {
                activityDao.updateSyncStatus(pendingActivities.map { it.activityId }, SyncStatus.SYNCED)
                return Result.success(0)
            }

            val rowValues = activitiesToUpload.map { entity ->
                entity.toGoogleSheetsRow().map { it.toString() }
            }

            val payload = ValueRange(
                range = "A1",
                majorDimension = "ROWS",
                values = rowValues
            )

            val response = googleSheetsApiService.appendValues(
                authorization = authHeader,
                spreadsheetId = sheetId,
                range = "A1",
                valueInputOption = "RAW",
                insertDataOption = "INSERT_ROWS",
                body = payload
            )

            if (response.updates != null || response.spreadsheetId != null) {
                val uploadedIds = activitiesToUpload.map { it.activityId }
                activityDao.updateSyncStatus(uploadedIds, SyncStatus.SYNCED)
                Result.success(activitiesToUpload.size)
            } else {
                Result.failure(RuntimeException("Google Sheets API hat keine Bestätigung zurückgegeben"))
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * Lädt die Datensätze aus Google Sheets.
     * Wenn limit <= 0, wird die gesamte Tabelle (A1:V) ohne Beschränkung geladen.
     */
    suspend fun fetchSheetPreviewData(sheetIdInput: String, limit: Int = 0): Result<List<List<String>>> {
        val sheetId = googleAuthManager.extractSpreadsheetId(sheetIdInput)
        if (sheetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Google Spreadsheet ID ist leer"))
        }

        val tokenResult = googleAuthManager.getValidAccessToken()
        val googleToken = tokenResult.getOrElse {
            return Result.failure(it)
        }

        return try {
            val authHeader = "Bearer $googleToken"
            val range = if (limit > 0) "A1:V$limit" else "A1:V"
            try {
                val response = googleSheetsApiService.getValues(
                    authorization = authHeader,
                    spreadsheetId = sheetId,
                    range = range
                )
                Result.success(response.rows)
            } catch (httpEx: HttpException) {
                if (httpEx.code() == 401) {
                    val refreshResult = googleAuthManager.refreshAccessToken()
                    if (refreshResult.isSuccess) {
                        val newToken = refreshResult.getOrThrow()
                        val retryResponse = googleSheetsApiService.getValues(
                            authorization = "Bearer $newToken",
                            spreadsheetId = sheetId,
                            range = range
                        )
                        return Result.success(retryResponse.rows)
                    } else {
                        val cause = refreshResult.exceptionOrNull()
                        return Result.failure(IllegalStateException("Google Token erneuern fehlgeschlagen: ${cause?.message ?: "Unbekannter Fehler"}"))
                    }
                }
                throw httpEx
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * Umfassende Integritätsprüfung:
     * 1. Sucht nach Dubletten in Google Sheets (mehrfache IDs in Spalte A)
     * 2. Findet fehlende Aktivitäten (in Room vorhanden, im Sheet fehlend)
     * 3. Erkennt Datenänderungen (z.B. neue Max-Kadenz / Kalorien lokal, aber im Sheet noch leer)
     */
    suspend fun checkDataIntegrity(sheetIdInput: String): Result<IntegrityReport> {
        val sheetId = googleAuthManager.extractSpreadsheetId(sheetIdInput)
        if (sheetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Google Spreadsheet ID ist leer"))
        }

        val tokenResult = googleAuthManager.getValidAccessToken()
        val googleToken = tokenResult.getOrElse {
            return Result.failure(it)
        }

        return try {
            val authHeader = "Bearer $googleToken"
            val response = googleSheetsApiService.getValues(
                authorization = authHeader,
                spreadsheetId = sheetId,
                range = "A:V"
            )

            val rows = response.rows
            val dataRows = if (rows.isNotEmpty() && rows[0].firstOrNull()?.trim() == "Activity ID") {
                rows.drop(1)
            } else {
                rows
            }

            val sheetIdList = mutableListOf<Long>()
            val duplicateIds = mutableSetOf<Long>()
            val sheetRowMap = mutableMapOf<Long, List<String>>()

            for (row in dataRows) {
                val id = row.firstOrNull()?.trim()?.toLongOrNull() ?: continue
                if (sheetIdList.contains(id)) {
                    duplicateIds.add(id)
                }
                sheetIdList.add(id)
                sheetRowMap[id] = row
            }

            val localActivities = activityDao.getAllActivitiesList()
            val sheetIdSet = sheetIdList.toSet()

            // 1. Fehlende Aktivitäten im Sheet
            val missing = localActivities.filter { it.activityId !in sheetIdSet }

            // 2. Datenänderungen (z.B. Kadenz Spalte O / Index 14 oder Kalorien Spalte U / Index 20)
            val outdated = mutableListOf<ActivityEntity>()
            for (local in localActivities) {
                val sheetRow = sheetRowMap[local.activityId] ?: continue
                val sheetMaxCadence = sheetRow.getOrNull(14)?.trim()?.toIntOrNull()
                val sheetCalories = sheetRow.getOrNull(20)?.trim()?.toIntOrNull()

                val cadenceUpdated = local.maxCadence != null && local.maxCadence != sheetMaxCadence
                val caloriesUpdated = local.kalorienKcal != null && local.kalorienKcal != sheetCalories

                if (cadenceUpdated || caloriesUpdated) {
                    outdated.add(local)
                }
            }

            Result.success(
                IntegrityReport(
                    missingInSheet = missing,
                    duplicateIdsInSheet = duplicateIds.toList(),
                    outdatedInSheet = outdated,
                    totalSheetRows = dataRows.size,
                    totalLocalActivities = localActivities.size
                )
            )
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * Schreibt die Tabelle komplett sauber und dublettenfrei neu.
     * Nutzt Google Sheets :clear gefolgt von einem einzelnen Bulk-Update.
     */
    suspend fun syncAllActivitiesWithoutDuplicates(sheetIdInput: String): Result<Int> {
        val sheetId = googleAuthManager.extractSpreadsheetId(sheetIdInput)
        if (sheetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Google Spreadsheet ID ist leer"))
        }

        val tokenResult = googleAuthManager.getValidAccessToken()
        val googleToken = tokenResult.getOrElse {
            return Result.failure(it)
        }

        val authHeader = "Bearer $googleToken"

        return try {
            val allLocal = activityDao.getAllActivitiesList()
            if (allLocal.isEmpty()) {
                return Result.success(0)
            }

            // Exakt 1 Zeile pro ID
            val uniqueLocal = allLocal.groupBy { it.activityId }
                .map { (_, group) -> group.maxByOrNull { it.fetchedAt } ?: group.first() }
                .sortedBy { it.activityId }

            // 1. Altes Blatt leeren (beseitigt alle Dubletten restlos)
            runCatching {
                googleSheetsApiService.clearValues(
                    authorization = authHeader,
                    spreadsheetId = sheetId,
                    range = "A:V"
                )
            }

            // 2. Kompletten frischen Datenbestand in einem Rutsch schreiben
            val allRows = listOf(DEFAULT_HEADERS) + uniqueLocal.map { entity ->
                entity.toGoogleSheetsRow().map { it.toString() }
            }

            val payload = ValueRange(
                range = "A1",
                majorDimension = "ROWS",
                values = allRows
            )

            val updateResponse = googleSheetsApiService.updateValues(
                authorization = authHeader,
                spreadsheetId = sheetId,
                range = "A1",
                valueInputOption = "RAW",
                body = payload
            )

            if (updateResponse.updatedRows != null && updateResponse.updatedRows > 0) {
                activityDao.updateSyncStatus(uniqueLocal.map { it.activityId }, SyncStatus.SYNCED)
                Result.success(uniqueLocal.size)
            } else {
                Result.failure(RuntimeException("Fehler beim Aktualisieren der Tabellendaten"))
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    suspend fun restoreMissingActivities(sheetIdInput: String, missingActivities: List<ActivityEntity>): Result<Int> {
        val sheetId = googleAuthManager.extractSpreadsheetId(sheetIdInput)
        if (sheetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Google Spreadsheet ID ist leer"))
        }

        if (missingActivities.isEmpty()) {
            return Result.success(0)
        }

        val tokenResult = googleAuthManager.getValidAccessToken()
        val googleToken = tokenResult.getOrElse {
            return Result.failure(it)
        }

        val authHeader = "Bearer $googleToken"

        return try {
            ensureSheetHeadersExist(authHeader, sheetId)

            val existing = googleSheetsApiService.getValues(
                authorization = authHeader,
                spreadsheetId = sheetId,
                range = "A:A"
            )
            val sheetIds = existing.rows.mapNotNull { it.firstOrNull()?.trim()?.toLongOrNull() }.toSet()

            val toAdd = missingActivities.filter { it.activityId !in sheetIds }
            if (toAdd.isEmpty()) {
                activityDao.updateSyncStatus(missingActivities.map { it.activityId }, SyncStatus.SYNCED)
                return Result.success(0)
            }

            val rowValues = toAdd.map { entity ->
                entity.toGoogleSheetsRow().map { it.toString() }
            }

            val payload = ValueRange(
                range = "A1",
                majorDimension = "ROWS",
                values = rowValues
            )

            val response = googleSheetsApiService.appendValues(
                authorization = authHeader,
                spreadsheetId = sheetId,
                range = "A1",
                valueInputOption = "RAW",
                insertDataOption = "INSERT_ROWS",
                body = payload
            )

            if (response.updates != null || response.spreadsheetId != null) {
                val restoredIds = toAdd.map { it.activityId }
                activityDao.updateSyncStatus(restoredIds, SyncStatus.SYNCED)
                Result.success(toAdd.size)
            } else {
                Result.failure(RuntimeException("Google Sheets API hat keine Bestätigung zurückgegeben"))
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    private suspend fun ensureSheetHeadersExist(authHeader: String, sheetId: String) {
        try {
            val existing = googleSheetsApiService.getValues(
                authorization = authHeader,
                spreadsheetId = sheetId,
                range = "A1:V1"
            )

            val firstRow = existing.rows.firstOrNull() ?: emptyList()
            val isHeaderPresent = firstRow.firstOrNull()?.equals(DEFAULT_HEADERS[0], ignoreCase = true) == true
            val isUpdated22Columns = firstRow.size >= 22 && firstRow.getOrNull(14) == "Max Cadence"

            if (!isHeaderPresent) {
                if (firstRow.isEmpty()) {
                    val headerPayload = ValueRange(
                        range = "A1:V1",
                        majorDimension = "ROWS",
                        values = listOf(DEFAULT_HEADERS)
                    )
                    googleSheetsApiService.updateValues(
                        authorization = authHeader,
                        spreadsheetId = sheetId,
                        range = "A1:V1",
                        valueInputOption = "RAW",
                        body = headerPayload
                    )
                } else {
                    val allExisting = googleSheetsApiService.getValues(
                        authorization = authHeader,
                        spreadsheetId = sheetId,
                        range = "A1:V"
                    )
                    val combinedRows = listOf(DEFAULT_HEADERS) + allExisting.rows
                    val headerPayload = ValueRange(
                        range = "A1",
                        majorDimension = "ROWS",
                        values = combinedRows
                    )
                    googleSheetsApiService.updateValues(
                        authorization = authHeader,
                        spreadsheetId = sheetId,
                        range = "A1",
                        valueInputOption = "RAW",
                        body = headerPayload
                    )
                }
            } else if (!isUpdated22Columns) {
                val headerPayload = ValueRange(
                    range = "A1:V1",
                    majorDimension = "ROWS",
                    values = listOf(DEFAULT_HEADERS)
                )
                googleSheetsApiService.updateValues(
                    authorization = authHeader,
                    spreadsheetId = sheetId,
                    range = "A1:V1",
                    valueInputOption = "RAW",
                    body = headerPayload
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mapException(e: Exception): Exception {
        return when (e) {
            is HttpException -> {
                when (e.code()) {
                    401 -> IllegalStateException("Authentifizierung fehlgeschlagen (401). Bitte Anmeldedaten prüfen.")
                    403 -> IllegalStateException("Zugriff verweigert (403). Fehlende Berechtigungen.")
                    404 -> IllegalArgumentException("Ressource nicht gefunden (404). Tabellen-ID prüfen.")
                    429 -> IllegalStateException("Rate-Limit überschritten (429). Bitte später erneut versuchen.")
                    else -> RuntimeException("Netzwerkfehler: HTTP ${e.code()} - ${e.message()}")
                }
            }
            else -> e
        }
    }

    companion object {
        val DEFAULT_HEADERS = listOf(
            "Activity ID",
            "Start Datum",
            "Start Uhrzeit",
            "Sportart",
            "Titel",
            "Indoor",
            "Distanz (km)",
            "Bewegungszeit (s)",
            "Bewegungszeit",
            "Höhenmeter (m)",
            "Gesamtzeit",
            "Ø Geschwindigkeit (km/h)",
            "Max Geschwindigkeit (km/h)",
            "Ø Cadence",
            "Max Cadence",
            "Ø HF",
            "Max HF",
            "Ø Leistung (W)",
            "NP (W)",
            "Energie (kJ)",
            "Kalorien (kcal)",
            "Suffer Score"
        )
    }
}
