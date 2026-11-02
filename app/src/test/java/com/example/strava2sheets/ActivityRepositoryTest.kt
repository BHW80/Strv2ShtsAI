package com.example.strava2sheets

import com.example.strava2sheets.data.local.ActivityDao
import com.example.strava2sheets.data.local.ActivityEntity
import com.example.strava2sheets.data.local.SyncStatus
import com.example.strava2sheets.data.remote.api.GoogleSheetsApiService
import com.example.strava2sheets.data.remote.api.StravaApiService
import com.example.strava2sheets.data.remote.auth.GoogleAuthManager
import com.example.strava2sheets.data.remote.auth.StravaAuthManager
import com.example.strava2sheets.data.remote.dto.AppendValuesResponse
import com.example.strava2sheets.data.remote.dto.ClearValuesResponse
import com.example.strava2sheets.data.remote.dto.UpdateValuesResponse
import com.example.strava2sheets.data.remote.dto.ValueRange
import com.example.strava2sheets.data.repository.ActivityRepository
import com.example.strava2sheets.data.security.TokenStorage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityRepositoryTest {

    @Test
    fun `fetchActivitiesFromStrava fails if Strava is not authenticated`() = runBlocking {
        val fakeDao = FakeActivityDao()
        val fakeStravaApi = FakeStravaApiService()
        val fakeSheetsApi = FakeGoogleSheetsApiService()
        val stravaAuthManager = StravaAuthManager(FakeTokenStorage(), fakeStravaApi)
        val googleAuthManager = GoogleAuthManager(FakeTokenStorage())

        val repository = ActivityRepository(
            activityDao = fakeDao,
            stravaApiService = fakeStravaApi,
            googleSheetsApiService = fakeSheetsApi,
            stravaAuthManager = stravaAuthManager,
            googleAuthManager = googleAuthManager
        )

        val result = repository.fetchActivitiesFromStrava()
        assertTrue(result.isFailure)
    }

    @Test
    fun `uploadPendingActivitiesToGoogleSheets fails if sheet ID is blank`() = runBlocking {
        val fakeDao = FakeActivityDao()
        val fakeStravaApi = FakeStravaApiService()
        val fakeSheetsApi = FakeGoogleSheetsApiService()
        val stravaAuthManager = StravaAuthManager(FakeTokenStorage(), fakeStravaApi)
        val googleAuthManager = GoogleAuthManager(FakeTokenStorage())

        val repository = ActivityRepository(
            activityDao = fakeDao,
            stravaApiService = fakeStravaApi,
            googleSheetsApiService = fakeSheetsApi,
            stravaAuthManager = stravaAuthManager,
            googleAuthManager = googleAuthManager
        )

        val result = repository.uploadPendingActivitiesToGoogleSheets("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `uploadPendingActivities inserts header if sheet is empty`() = runBlocking {
        val activity = ActivityEntity(
            activityId = 1001L,
            startDatum = "2026-03-29",
            startUhrzeit = "10:00:00",
            sportart = "Run",
            titel = "Morning Run",
            istIndoor = false,
            distanzKm = 10.0,
            bewegungszeitSek = 3000,
            bewegungszeitFormatted = "00:50:00",
            hoehenmeterM = 50,
            gesamtzeitFormatted = "00:50:00",
            avgGeschwindigkeitKmh = 12.0,
            maxGeschwindigkeitKmh = 15.0,
            avgCadence = 160,
            maxCadence = 175,
            avgHF = 140,
            maxHF = 160,
            avgLeistungWatt = null,
            npleistungWatt = null,
            energieKj = 500,
            kalorienKcal = 400,
            sufferScore = 40,
            syncStatus = SyncStatus.PENDING
        )

        val fakeDao = object : ActivityDao by FakeActivityDao() {
            override suspend fun getActivitiesByStatus(status: SyncStatus, limit: Int): List<ActivityEntity> {
                return if (status == SyncStatus.PENDING) listOf(activity) else emptyList()
            }
        }

        val fakeStravaApi = FakeStravaApiService()
        val fakeSheetsApi = FakeGoogleSheetsApiService()

        val tokenStorage = FakeTokenStorage()
        tokenStorage.saveGoogleAccessToken("fake_token")
        val googleAuthManager = GoogleAuthManager(tokenStorage)
        googleAuthManager.saveSpreadsheetId("fake_sheet_id")
        val stravaAuthManager = StravaAuthManager(tokenStorage, fakeStravaApi)

        val repository = ActivityRepository(
            activityDao = fakeDao,
            stravaApiService = fakeStravaApi,
            googleSheetsApiService = fakeSheetsApi,
            stravaAuthManager = stravaAuthManager,
            googleAuthManager = googleAuthManager
        )

        val result = repository.uploadPendingActivitiesToGoogleSheets("fake_sheet_id")
        assertTrue(result.isSuccess)

        // Verify row 0 is header and row 1 is activity data
        assertEquals(2, fakeSheetsApi.sheetGrid.size)
        assertEquals("Activity ID", fakeSheetsApi.sheetGrid[0][0])
        assertEquals("1001", fakeSheetsApi.sheetGrid[1][0])
    }

    @Test
    fun `uploadPendingActivities prepends header if sheet has data without header`() = runBlocking {
        val activity = ActivityEntity(
            activityId = 2002L,
            startDatum = "2026-03-29",
            startUhrzeit = "11:00:00",
            sportart = "Ride",
            titel = "Lunch Ride",
            istIndoor = false,
            distanzKm = 20.0,
            bewegungszeitSek = 3600,
            bewegungszeitFormatted = "01:00:00",
            hoehenmeterM = 100,
            gesamtzeitFormatted = "01:00:00",
            avgGeschwindigkeitKmh = 20.0,
            maxGeschwindigkeitKmh = 30.0,
            avgCadence = 80,
            maxCadence = 100,
            avgHF = 130,
            maxHF = 150,
            avgLeistungWatt = 200,
            npleistungWatt = 210,
            energieKj = 700,
            kalorienKcal = 600,
            sufferScore = 50,
            syncStatus = SyncStatus.PENDING
        )

        val fakeDao = object : ActivityDao by FakeActivityDao() {
            override suspend fun getActivitiesByStatus(status: SyncStatus, limit: Int): List<ActivityEntity> {
                return if (status == SyncStatus.PENDING) listOf(activity) else emptyList()
            }
        }

        val fakeStravaApi = FakeStravaApiService()
        val fakeSheetsApi = FakeGoogleSheetsApiService()
        // Pre-populate sheet with a data row without headers
        fakeSheetsApi.sheetGrid.add(listOf("9999", "2026-01-01", "08:00:00", "Run"))

        val tokenStorage = FakeTokenStorage()
        tokenStorage.saveGoogleAccessToken("fake_token")
        val googleAuthManager = GoogleAuthManager(tokenStorage)
        googleAuthManager.saveSpreadsheetId("fake_sheet_id")
        val stravaAuthManager = StravaAuthManager(tokenStorage, fakeStravaApi)

        val repository = ActivityRepository(
            activityDao = fakeDao,
            stravaApiService = fakeStravaApi,
            googleSheetsApiService = fakeSheetsApi,
            stravaAuthManager = stravaAuthManager,
            googleAuthManager = googleAuthManager
        )

        val result = repository.uploadPendingActivitiesToGoogleSheets("fake_sheet_id")
        assertTrue(result.isSuccess)

        // Verify row 0 is header, row 1 is existing data, row 2 is new activity
        assertEquals(3, fakeSheetsApi.sheetGrid.size)
        assertEquals("Activity ID", fakeSheetsApi.sheetGrid[0][0])
        assertEquals("9999", fakeSheetsApi.sheetGrid[1][0])
        assertEquals("2002", fakeSheetsApi.sheetGrid[2][0])
    }

    @Test
    fun `uploadPendingActivities does not duplicate header if header already present`() = runBlocking {
        val activity = ActivityEntity(
            activityId = 3003L,
            startDatum = "2026-03-29",
            startUhrzeit = "12:00:00",
            sportart = "Swim",
            titel = "Pool Swim",
            istIndoor = true,
            distanzKm = 2.0,
            bewegungszeitSek = 1800,
            bewegungszeitFormatted = "00:30:00",
            hoehenmeterM = 0,
            gesamtzeitFormatted = "00:30:00",
            avgGeschwindigkeitKmh = 4.0,
            maxGeschwindigkeitKmh = 5.0,
            avgCadence = null,
            maxCadence = null,
            avgHF = 135,
            maxHF = 155,
            avgLeistungWatt = null,
            npleistungWatt = null,
            energieKj = 300,
            kalorienKcal = 250,
            sufferScore = 25,
            syncStatus = SyncStatus.PENDING
        )

        val fakeDao = object : ActivityDao by FakeActivityDao() {
            override suspend fun getActivitiesByStatus(status: SyncStatus, limit: Int): List<ActivityEntity> {
                return if (status == SyncStatus.PENDING) listOf(activity) else emptyList()
            }
        }

        val fakeStravaApi = FakeStravaApiService()
        val fakeSheetsApi = FakeGoogleSheetsApiService()
        // Pre-populate sheet with header and 1 data row
        fakeSheetsApi.sheetGrid.add(ActivityRepository.DEFAULT_HEADERS)
        fakeSheetsApi.sheetGrid.add(listOf("1001", "2026-03-29", "10:00:00", "Run"))

        val tokenStorage = FakeTokenStorage()
        tokenStorage.saveGoogleAccessToken("fake_token")
        val googleAuthManager = GoogleAuthManager(tokenStorage)
        googleAuthManager.saveSpreadsheetId("fake_sheet_id")
        val stravaAuthManager = StravaAuthManager(tokenStorage, fakeStravaApi)

        val repository = ActivityRepository(
            activityDao = fakeDao,
            stravaApiService = fakeStravaApi,
            googleSheetsApiService = fakeSheetsApi,
            stravaAuthManager = stravaAuthManager,
            googleAuthManager = googleAuthManager
        )

        val result = repository.uploadPendingActivitiesToGoogleSheets("fake_sheet_id")
        assertTrue(result.isSuccess)

        // Verify only 1 header row exists at row 0
        assertEquals(3, fakeSheetsApi.sheetGrid.size)
        assertEquals("Activity ID", fakeSheetsApi.sheetGrid[0][0])
        assertEquals("1001", fakeSheetsApi.sheetGrid[1][0])
        assertEquals("3003", fakeSheetsApi.sheetGrid[2][0])
    }

    @Test
    fun `checkDataIntegrity correctly identifies missing local activities`() = runBlocking {
        val activity1 = ActivityEntity(
            activityId = 101L,
            startDatum = "2026-03-29",
            startUhrzeit = "10:00:00",
            sportart = "Run",
            titel = "Morning Run",
            istIndoor = false,
            distanzKm = 5.0,
            bewegungszeitSek = 1500,
            bewegungszeitFormatted = "00:25:00",
            hoehenmeterM = 10,
            gesamtzeitFormatted = "00:25:00",
            avgGeschwindigkeitKmh = 12.0,
            maxGeschwindigkeitKmh = 14.0,
            avgCadence = 160,
            maxCadence = 180,
            avgHF = 140,
            maxHF = 155,
            avgLeistungWatt = null,
            npleistungWatt = null,
            energieKj = 300,
            kalorienKcal = 350,
            sufferScore = 20,
            syncStatus = SyncStatus.SYNCED
        )

        val activity2 = ActivityEntity(
            activityId = 202L,
            startDatum = "2026-03-29",
            startUhrzeit = "11:00:00",
            sportart = "Ride",
            titel = "Lunch Ride",
            istIndoor = false,
            distanzKm = 15.0,
            bewegungszeitSek = 2700,
            bewegungszeitFormatted = "00:45:00",
            hoehenmeterM = 50,
            gesamtzeitFormatted = "00:45:00",
            avgGeschwindigkeitKmh = 20.0,
            maxGeschwindigkeitKmh = 28.0,
            avgCadence = 85,
            maxCadence = 105,
            avgHF = 135,
            maxHF = 150,
            avgLeistungWatt = 180,
            npleistungWatt = 190,
            energieKj = 500,
            kalorienKcal = 450,
            sufferScore = 35,
            syncStatus = SyncStatus.SYNCED
        )

        val fakeDao = object : ActivityDao by FakeActivityDao() {
            override suspend fun getAllActivitiesList(): List<ActivityEntity> {
                return listOf(activity1, activity2)
            }
        }

        val fakeStravaApi = FakeStravaApiService()
        val fakeSheetsApi = FakeGoogleSheetsApiService()
        fakeSheetsApi.sheetGrid.add(ActivityRepository.DEFAULT_HEADERS)
        fakeSheetsApi.sheetGrid.add(listOf("101", "2026-03-29", "10:00:00", "Run"))

        val tokenStorage = FakeTokenStorage()
        tokenStorage.saveGoogleAccessToken("fake_token")
        val googleAuthManager = GoogleAuthManager(tokenStorage)
        googleAuthManager.saveSpreadsheetId("fake_sheet_id")
        val stravaAuthManager = StravaAuthManager(tokenStorage, fakeStravaApi)

        val repository = ActivityRepository(
            activityDao = fakeDao,
            stravaApiService = fakeStravaApi,
            googleSheetsApiService = fakeSheetsApi,
            stravaAuthManager = stravaAuthManager,
            googleAuthManager = googleAuthManager
        )

        val result = repository.checkDataIntegrity("fake_sheet_id")
        assertTrue(result.isSuccess)

        val report = result.getOrNull()
        assertEquals(1, report?.missingInSheet?.size)
        assertEquals(202L, report?.missingInSheet?.first()?.activityId)
    }

    @Test
    fun `restoreMissingActivities appends missing rows to sheet`() = runBlocking {
        val missingActivity = ActivityEntity(
            activityId = 202L,
            startDatum = "2026-03-29",
            startUhrzeit = "11:00:00",
            sportart = "Ride",
            titel = "Lunch Ride",
            istIndoor = false,
            distanzKm = 15.0,
            bewegungszeitSek = 2700,
            bewegungszeitFormatted = "00:45:00",
            hoehenmeterM = 50,
            gesamtzeitFormatted = "00:45:00",
            avgGeschwindigkeitKmh = 20.0,
            maxGeschwindigkeitKmh = 28.0,
            avgCadence = 85,
            maxCadence = 105,
            avgHF = 135,
            maxHF = 150,
            avgLeistungWatt = 180,
            npleistungWatt = 190,
            energieKj = 500,
            kalorienKcal = 450,
            sufferScore = 35,
            syncStatus = SyncStatus.SYNCED
        )

        val fakeDao = FakeActivityDao()
        val fakeStravaApi = FakeStravaApiService()
        val fakeSheetsApi = FakeGoogleSheetsApiService()
        fakeSheetsApi.sheetGrid.add(ActivityRepository.DEFAULT_HEADERS)
        fakeSheetsApi.sheetGrid.add(listOf("101", "2026-03-29", "10:00:00", "Run"))

        val tokenStorage = FakeTokenStorage()
        tokenStorage.saveGoogleAccessToken("fake_token")
        val googleAuthManager = GoogleAuthManager(tokenStorage)
        googleAuthManager.saveSpreadsheetId("fake_sheet_id")
        val stravaAuthManager = StravaAuthManager(tokenStorage, fakeStravaApi)

        val repository = ActivityRepository(
            activityDao = fakeDao,
            stravaApiService = fakeStravaApi,
            googleSheetsApiService = fakeSheetsApi,
            stravaAuthManager = stravaAuthManager,
            googleAuthManager = googleAuthManager
        )

        val result = repository.restoreMissingActivities("fake_sheet_id", listOf(missingActivity))
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())

        assertEquals(3, fakeSheetsApi.sheetGrid.size)
        assertEquals("202", fakeSheetsApi.sheetGrid[2][0])
    }

    @Test
    fun `fetchSheetPreviewData fetches sheet rows via REST API`() = runBlocking {
        val fakeDao = FakeActivityDao()
        val fakeStravaApi = FakeStravaApiService()
        val fakeSheetsApi = FakeGoogleSheetsApiService()
        fakeSheetsApi.sheetGrid.add(ActivityRepository.DEFAULT_HEADERS)
        fakeSheetsApi.sheetGrid.add(listOf("101", "2026-03-29", "10:00:00", "Run"))

        val tokenStorage = FakeTokenStorage()
        tokenStorage.saveGoogleAccessToken("fake_token")
        val googleAuthManager = GoogleAuthManager(tokenStorage)
        googleAuthManager.saveSpreadsheetId("fake_sheet_id")
        val stravaAuthManager = StravaAuthManager(tokenStorage, fakeStravaApi)

        val repository = ActivityRepository(
            activityDao = fakeDao,
            stravaApiService = fakeStravaApi,
            googleSheetsApiService = fakeSheetsApi,
            stravaAuthManager = stravaAuthManager,
            googleAuthManager = googleAuthManager
        )

        val result = repository.fetchSheetPreviewData("fake_sheet_id")
        assertTrue(result.isSuccess)
        val rows = result.getOrNull()
        assertEquals(2, rows?.size)
        assertEquals("Activity ID", rows?.get(0)?.get(0))
        assertEquals("101", rows?.get(1)?.get(0))
    }
}

class FakeActivityDao : ActivityDao {
    override suspend fun insertOrIgnoreActivities(activities: List<ActivityEntity>): List<Long> = emptyList()
    override suspend fun insertOrUpdateActivity(activity: ActivityEntity) {}
    override suspend fun getActivitiesByStatus(status: SyncStatus, limit: Int): List<ActivityEntity> = emptyList()
    override suspend fun updateSyncStatus(activityIds: List<Long>, status: SyncStatus) {}
    override suspend fun getLatestFetchedTimestamp(): Long? = null
    override suspend fun getHighestActivityId(): Long? = null
    override fun getAllActivitiesFlow(): kotlinx.coroutines.flow.Flow<List<ActivityEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun getAllActivitiesList(): List<ActivityEntity> = emptyList()
    override suspend fun getCountByStatus(status: SyncStatus): Int = 0
    override suspend fun clearAll() {}
}

class FakeStravaApiService : StravaApiService {
    override suspend fun exchangeToken(clientId: String, clientSecret: String, code: String, grantType: String): com.example.strava2sheets.data.remote.dto.StravaTokenResponse {
        throw UnsupportedOperationException()
    }
    override suspend fun refreshToken(clientId: String, clientSecret: String, refreshToken: String, grantType: String): com.example.strava2sheets.data.remote.dto.StravaTokenResponse {
        throw UnsupportedOperationException()
    }
    override suspend fun getAthleteActivities(authorization: String, afterEpochSeconds: Long?, page: Int, perPage: Int): List<com.example.strava2sheets.data.remote.dto.StravaActivityDto> {
        return emptyList()
    }
    override suspend fun getActivityDetailRaw(authorization: String, id: Long, includeAllEfforts: Boolean): JsonElement {
        return JsonObject(emptyMap())
    }
    override suspend fun getActivityStreamsRaw(authorization: String, id: Long, keys: String, keyByType: Boolean): JsonElement {
        return JsonObject(emptyMap())
    }
    override suspend fun getAuthenticatedAthlete(authorization: String): com.example.strava2sheets.data.remote.dto.StravaAthleteDto {
        return com.example.strava2sheets.data.remote.dto.StravaAthleteDto(id = 12345L)
    }
    override suspend fun getAthleteZones(authorization: String): com.example.strava2sheets.data.remote.dto.StravaAthleteZonesDto {
        return com.example.strava2sheets.data.remote.dto.StravaAthleteZonesDto()
    }
}

class FakeGoogleSheetsApiService : GoogleSheetsApiService {
    var sheetGrid: MutableList<List<String>> = mutableListOf()

    override suspend fun getValues(authorization: String, spreadsheetId: String, range: String): ValueRange {
        if (range == "A1:V1") {
            val firstRow = sheetGrid.firstOrNull()
            return ValueRange(range = "A1:V1", values = if (firstRow != null) listOf(firstRow) else emptyList())
        }
        return ValueRange(range = range, values = sheetGrid)
    }

    override suspend fun updateValues(
        authorization: String,
        spreadsheetId: String,
        range: String,
        valueInputOption: String,
        body: ValueRange
    ): UpdateValuesResponse {
        if (range == "A1:V1") {
            if (sheetGrid.isEmpty()) {
                sheetGrid.addAll(body.rows)
            } else {
                sheetGrid[0] = body.rows.first()
            }
        } else if (range == "A1") {
            sheetGrid = body.rows.toMutableList()
        }
        return UpdateValuesResponse(spreadsheetId = spreadsheetId, updatedRange = range)
    }

    override suspend fun appendValues(
        authorization: String,
        spreadsheetId: String,
        range: String,
        valueInputOption: String,
        insertDataOption: String,
        body: ValueRange
    ): AppendValuesResponse {
        sheetGrid.addAll(body.rows)
        return AppendValuesResponse(spreadsheetId = spreadsheetId, updates = UpdateValuesResponse(updatedRows = body.rows.size))
    }

    override suspend fun clearValues(
        authorization: String,
        spreadsheetId: String,
        range: String,
        body: Map<String, String>
    ): ClearValuesResponse {
        sheetGrid.clear()
        return ClearValuesResponse(spreadsheetId = spreadsheetId, clearedRange = range)
    }
}

class FakeTokenStorage : TokenStorage(null) {
    private var googleToken: String? = null

    override fun getGoogleAccessToken(): String? = googleToken

    override fun saveGoogleAccessToken(token: String, expiresAt: Long) {
        googleToken = token
    }

    override fun isGoogleTokenExpired(): Boolean = false
}
