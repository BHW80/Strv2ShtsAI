package com.example.strava2sheets.ui.dashboard

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.strava2sheets.ui.common.AppBackground

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    DashboardContent(
        uiState = uiState,
        context = context,
        onStartSync = { viewModel.startSync() },
        onRefetchAllFromStrava = { viewModel.reFetchAllFromStrava() },
        onCheckIntegrity = { viewModel.checkDataIntegrity() },
        onSyncAllWithoutDuplicates = { viewModel.syncAllWithoutDuplicates() },
        onRestoreMissingActivities = { viewModel.restoreMissingActivities() },
        onFetchAthleteProfile = { viewModel.fetchAthleteProfile() },
        onSaveStrava = { clientId, clientSecret ->
            viewModel.startStravaAuth(context, clientId, clientSecret)
        },
        onDisconnectStrava = { viewModel.disconnectStrava() },
        onSaveGoogleCredentials = { accessToken, refreshToken, clientId, clientSecret ->
            viewModel.saveGoogleCredentials(accessToken, refreshToken, clientId, clientSecret)
        },
        onDisconnectGoogle = { viewModel.disconnectGoogle() },
        onSaveSpreadsheetId = { sheetId -> viewModel.saveSpreadsheetId(sheetId) },
        onRefreshPreview = { viewModel.loadSheetPreview() },
        modifier = modifier
    )
}

@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    context: Context,
    onStartSync: () -> Unit,
    onRefetchAllFromStrava: () -> Unit,
    onCheckIntegrity: () -> Unit,
    onSyncAllWithoutDuplicates: () -> Unit,
    onRestoreMissingActivities: () -> Unit,
    onFetchAthleteProfile: () -> Unit,
    onSaveStrava: (String, String) -> Unit,
    onDisconnectStrava: () -> Unit,
    onSaveGoogleCredentials: (String, String, String, String) -> Unit,
    onDisconnectGoogle: () -> Unit,
    onSaveSpreadsheetId: (String) -> Unit,
    onRefreshPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stravaExpanded by rememberSaveable { mutableStateOf(false) }
    var googleExpanded by rememberSaveable { mutableStateOf(false) }
    var previewExpanded by rememberSaveable { mutableStateOf(false) }
    var integrityExpanded by rememberSaveable { mutableStateOf(false) }
    var athleteExpanded by rememberSaveable { mutableStateOf(false) } // Neuer Abschnitt Nutzerdaten

    var stravaIdInput by rememberSaveable { mutableStateOf(uiState.stravaClientId) }
    var stravaSecretInput by rememberSaveable { mutableStateOf(uiState.stravaClientSecret) }

    var googleAccessTokenInput by rememberSaveable { mutableStateOf(uiState.googleAccessTokenInput) }
    var googleRefreshTokenInput by rememberSaveable { mutableStateOf(uiState.googleRefreshTokenInput) }
    var googleClientIdInput by rememberSaveable { mutableStateOf(uiState.googleClientIdInput) }
    var googleClientSecretInput by rememberSaveable { mutableStateOf(uiState.googleClientSecretInput) }

    var sheetIdInput by rememberSaveable { mutableStateOf(uiState.spreadsheetId) }

    AppBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / Title: Start
            Text(
                text = "Start",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Sync Status & Actions Card (Hauptbereich)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Synchronisations-Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatCounter(count = uiState.pendingCount, label = "Ausstehend")
                        StatCounter(count = uiState.syncedCount, label = "Synchronisiert")
                        StatCounter(count = uiState.failedCount, label = "Fehlgeschlagen")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.isSyncing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.syncStatusMessage.isNotBlank()) {
                        Text(
                            text = uiState.syncStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Button 1: Jetzt mit Sheets synchronisieren
                    Button(
                        onClick = { onStartSync() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jetzt mit Sheets synchronisieren")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 2: Alle Aktivitäten von Strava holen
                    OutlinedButton(
                        onClick = { onRefetchAllFromStrava() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Alle Aktivitäten von Strava holen")
                    }
                }
            }

            // Neuer Abschnitt: Nutzerdaten (expandable, standardmäßig zugeklappt)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(tween(250, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { athleteExpanded = !athleteExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Nutzerdaten",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            uiState.latestAthleteProfile?.let { p ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${p.weightKg ?: "?"} kg • ${p.ftpWatts ?: "?"} W",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = if (athleteExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (athleteExpanded) "Einklappen" else "Ausklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = athleteExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Rufe dein aktuelles Körpergewicht, deine FTP und Zonen von Strava ab. Bei mehrmaliger täglicher Aktualisierung wird der Eintrag für das heutige Datum sauber aktualisiert.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            uiState.latestAthleteProfile?.let { profile ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Aktueller Stand (${profile.date}):",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Gewicht: ${profile.weightKg?.let { "$it kg" } ?: "nicht hinterlegt"}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "FTP: ${profile.ftpWatts?.let { "$it W" } ?: "nicht hinterlegt"}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "W/kg: ${profile.calculatedWPerKg?.let { "$it W/kg" } ?: "-"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (uiState.isFetchingAthlete) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (uiState.athleteFetchMessage.isNotBlank()) {
                                Text(
                                    text = uiState.athleteFetchMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Button(
                                onClick = { onFetchAthleteProfile() },
                                enabled = !uiState.isFetchingAthlete && uiState.isStravaConnected,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (uiState.isFetchingAthlete) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Nutzerdaten aktualisieren")
                            }
                        }
                    }
                }
            }

            // Section 1: Expandable Strava API Konfiguration (Standard: eingeklappt)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(tween(250, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { stravaExpanded = !stravaExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Strava API Konfiguration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            StatusBadge(isConnected = uiState.isStravaConnected)
                        }
                        Icon(
                            imageVector = if (stravaExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (stravaExpanded) "Einklappen" else "Ausklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = stravaExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = stravaIdInput,
                                onValueChange = { stravaIdInput = it },
                                label = { Text("Strava Client ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = stravaSecretInput,
                                onValueChange = { stravaSecretInput = it },
                                label = { Text("Strava Client Secret") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onSaveStrava(stravaIdInput, stravaSecretInput) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (uiState.isStravaConnected) "Neu verbinden" else "Mit Strava verbinden")
                                }

                                if (uiState.isStravaConnected) {
                                    OutlinedButton(
                                        onClick = { onDisconnectStrava() },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Trennen")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Expandable Google Konfiguration (Standard: eingeklappt)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(tween(250, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { googleExpanded = !googleExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Google Konfiguration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            StatusBadge(isConnected = uiState.isGoogleConnected)
                        }
                        Icon(
                            imageVector = if (googleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (googleExpanded) "Einklappen" else "Ausklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = googleExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = sheetIdInput,
                                onValueChange = { sheetIdInput = it },
                                label = { Text("Google Spreadsheet ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = googleAccessTokenInput,
                                onValueChange = { googleAccessTokenInput = it },
                                label = { Text("Google Access Token (OAuth 2.0 Bearer)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = googleRefreshTokenInput,
                                onValueChange = { googleRefreshTokenInput = it },
                                label = { Text("Google Refresh Token (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = googleClientIdInput,
                                    onValueChange = { googleClientIdInput = it },
                                    label = { Text("Client ID (opt.)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = googleClientSecretInput,
                                    onValueChange = { googleClientSecretInput = it },
                                    label = { Text("Client Secret (opt.)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onSaveSpreadsheetId(sheetIdInput)
                                        onSaveGoogleCredentials(
                                            googleAccessTokenInput,
                                            googleRefreshTokenInput,
                                            googleClientIdInput,
                                            googleClientSecretInput
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Speichern")
                                }

                                if (uiState.isGoogleConnected) {
                                    OutlinedButton(
                                        onClick = { onDisconnectGoogle() },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Trennen")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Expandable Datenintegrität & Bereinigung (Standard: eingeklappt)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(tween(250, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { integrityExpanded = !integrityExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Datenintegrität & Bereinigung",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (integrityExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (integrityExpanded) "Einklappen" else "Ausklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = integrityExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Prüft das Google Sheet auf Dubletten und Abweichungen und stellt eine saubere, deduplizierte Synchronisation sicher.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (uiState.isCheckingIntegrity || uiState.isRestoringData) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (uiState.integrityStatusMessage.isNotBlank()) {
                                Text(
                                    text = uiState.integrityStatusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.integrityReport?.hasIssues == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Button(
                                onClick = { onCheckIntegrity() },
                                enabled = !uiState.isCheckingIntegrity && !uiState.isRestoringData && uiState.isGoogleConnected,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Tabelle auf Dubletten & Differenzen prüfen")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { onSyncAllWithoutDuplicates() },
                                enabled = !uiState.isCheckingIntegrity && !uiState.isRestoringData && uiState.isGoogleConnected,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Komplett synchronisieren (Dubletten bereinigen)")
                            }

                            if (uiState.missingActivities.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onRestoreMissingActivities() },
                                    enabled = !uiState.isRestoringData,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${uiState.missingActivities.size} fehlende Zeilen jetzt nachtragen")
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Expandable Tabellenvorschau (Standard: eingeklappt)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(tween(250, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { previewExpanded = !previewExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tabellenvorschau (Google Sheets)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (previewExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (previewExpanded) "Einklappen" else "Ausklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = previewExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            if (uiState.isLoadingPreview) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (uiState.previewData.isNotEmpty()) {
                                val scrollState = rememberScrollState()
                                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                                    Column {
                                        uiState.previewData.forEachIndexed { rowIndex, row ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                row.take(6).forEach { cell ->
                                                    Text(
                                                        text = cell,
                                                        style = if (rowIndex == 0) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                                                        fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier.width(100.dp)
                                                    )
                                                }
                                            }
                                            if (rowIndex == 0) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Keine Vorschau verfügbar. Bitte Google Sheets verbinden.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { onRefreshPreview() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Vorschau aktualisieren")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCounter(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun StatusBadge(isConnected: Boolean) {
    Surface(
        color = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (isConnected) "Verbunden" else "Getrennt",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}
