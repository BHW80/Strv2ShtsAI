package com.example.strava2sheets.ui.activities

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.strava2sheets.data.local.ActivityEntity
import com.example.strava2sheets.data.local.AthleteProfileEntity
import com.example.strava2sheets.data.local.SyncStatus
import com.example.strava2sheets.ui.common.AppBackground
import com.example.strava2sheets.ui.dashboard.DashboardViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class ActivitySortBy(val label: String) {
    DATE("Datum & Zeit"),
    DISTANCE("Distanz"),
    DURATION("Dauer"),
    TITLE("Titel")
}

enum class SortDirection(val label: String) {
    DESC("Absteigend (Zuerst neu / hoch)"),
    ASC("Aufsteigend (Zuerst alt / niedrig)")
}

@Composable
fun ActivityListScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    ActivityListContent(
        activities = uiState.activities,
        pendingCount = uiState.pendingCount,
        syncedCount = uiState.syncedCount,
        failedCount = uiState.failedCount,
        athleteProfile = uiState.latestAthleteProfile,
        modifier = modifier
    )
}

@Composable
fun ActivityListContent(
    activities: List<ActivityEntity>,
    pendingCount: Int = 0,
    syncedCount: Int = 0,
    failedCount: Int = 0,
    athleteProfile: com.example.strava2sheets.data.local.AthleteProfileEntity? = null,
    modifier: Modifier = Modifier
) {
    var filterExpanded by rememberSaveable { mutableStateOf(true) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf<SyncStatus?>(null) }
    var sortBy by rememberSaveable { mutableStateOf(ActivitySortBy.DATE) }
    var sortDirection by rememberSaveable { mutableStateOf(SortDirection.DESC) }
    var expandedActivityId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val processedActivities = remember(activities, searchQuery, selectedFilter, sortBy, sortDirection) {
        var list = activities

        // 1. Filter by SyncStatus
        if (selectedFilter != null) {
            list = list.filter { it.syncStatus == selectedFilter }
        }

        // 2. Filter by Search Query
        val query = searchQuery.trim().lowercase()
        if (query.isNotBlank()) {
            list = list.filter {
                it.titel.lowercase().contains(query) ||
                        it.sportart.lowercase().contains(query) ||
                        it.startDatum.lowercase().contains(query)
            }
        }

        // 3. Sorting
        val sorted = when (sortBy) {
            ActivitySortBy.DATE -> list.sortedWith(
                compareBy<ActivityEntity> { it.startDatum }.thenBy { it.startUhrzeit }
            )
            ActivitySortBy.DISTANCE -> list.sortedBy { it.distanzKm ?: 0.0 }
            ActivitySortBy.DURATION -> list.sortedBy { it.bewegungszeitSek }
            ActivitySortBy.TITLE -> list.sortedBy { it.titel.lowercase() }
        }

        if (sortDirection == SortDirection.DESC) {
            sorted.reversed()
        } else {
            sorted
        }
    }

    AppBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filterExpanded = !filterExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktivitäten (${processedActivities.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (filterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (filterExpanded) "Filter minimieren" else "Filter maximieren",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            AnimatedVisibility(
                visible = filterExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Suchen (Titel, Sportart, Datum)...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Suchen")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Löschen")
                                }
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Filter Chips Row (Full width)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                label = { Text("Alle (${activities.size})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SyncStatus.PENDING,
                                onClick = { selectedFilter = SyncStatus.PENDING },
                                label = { Text("Ausstehend ($pendingCount)") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SyncStatus.SYNCED,
                                onClick = { selectedFilter = SyncStatus.SYNCED },
                                label = { Text("Synchronisiert ($syncedCount)") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == SyncStatus.FAILED,
                                onClick = { selectedFilter = SyncStatus.FAILED },
                                label = { Text("Fehler ($failedCount)") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sort Row below Status Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sortierung",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Sort Dropdown Button
                        Box {
                            OutlinedButton(
                                onClick = { sortMenuOpen = true }
                            ) {
                                Text(
                                    text = "${sortBy.label} (${if (sortDirection == SortDirection.DESC) "↓" else "↑"})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Sortieren",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuOpen,
                                onDismissRequest = { sortMenuOpen = false }
                            ) {
                                ActivitySortBy.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label + if (sortBy == option) " ✓" else "",
                                                fontWeight = if (sortBy == option) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            if (sortBy == option) {
                                                sortDirection = if (sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
                                            } else {
                                                sortBy = option
                                                sortDirection = SortDirection.DESC
                                            }
                                            sortMenuOpen = false
                                        }
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Richtung wechseln (${if (sortDirection == SortDirection.DESC) "Aktuell: Absteigend" else "Aktuell: Aufsteigend"})"
                                        )
                                    },
                                    onClick = {
                                        sortDirection = if (sortDirection == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (processedActivities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "Keine Aktivitäten entsprechen den Filterkriterien.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(processedActivities, key = { it.activityId }) { activity ->
                        val isExpanded = expandedActivityId == activity.activityId
                        ActivityItemCard(
                            activity = activity,
                            isExpanded = isExpanded,
                            athleteProfile = athleteProfile,
                            onToggleExpand = {
                                expandedActivityId = if (isExpanded) null else activity.activityId
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItemCard(
    activity: ActivityEntity,
    isExpanded: Boolean,
    athleteProfile: com.example.strava2sheets.data.local.AthleteProfileEntity? = null,
    onToggleExpand: () -> Unit
) {
    var zonesExpanded by rememberSaveable(activity.activityId) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleExpand() }
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 280,
                    easing = FastOutSlowInEasing
                )
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title & Sync Badge + Expand Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.titel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SyncBadge(status = activity.syncStatus)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Einklappen" else "Ausklappen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle: Sport type, Indoor/Outdoor, Date & Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${activity.sportart}${if (activity.istIndoor) " (Indoor)" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${activity.startDatum} um ${activity.startUhrzeit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary Row (Always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Distanz: ${activity.distanzKm?.let { "%.2f km".format(it) } ?: "- km"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Dauer: ${activity.bewegungszeitFormatted}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Höhe: ${activity.hoehenmeterM?.let { "$it m" } ?: "- m"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Expanded Detail View
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Alle Trainingsparameter",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Parameter Grid (2 Columns)
                    ParameterRow(
                        label1 = "Aktivität-ID",
                        value1 = activity.activityId.toString(),
                        label2 = "Umgebung",
                        value2 = if (activity.istIndoor) "Indoor" else "Outdoor"
                    )

                    ParameterRow(
                        label1 = "Bewegungszeit",
                        value1 = activity.bewegungszeitFormatted,
                        label2 = "Gesamtzeit",
                        value2 = activity.gesamtzeitFormatted.ifBlank { "-" }
                    )

                    ParameterRow(
                        label1 = "Ø Geschwindigkeit",
                        value1 = activity.avgGeschwindigkeitKmh?.let { "%.1f km/h".format(it) } ?: "-",
                        label2 = "Max. Geschwindigkeit",
                        value2 = activity.maxGeschwindigkeitKmh?.let { "%.1f km/h".format(it) } ?: "-"
                    )

                    ParameterRow(
                        label1 = "Ø Herzfrequenz",
                        value1 = activity.avgHF?.let { "$it bpm" } ?: "-",
                        label2 = "Max. Herzfrequenz",
                        value2 = activity.maxHF?.let { "$it bpm" } ?: "-"
                    )

                    ParameterRow(
                        label1 = "Ø Leistung (Watt)",
                        value1 = activity.avgLeistungWatt?.let { "$it W" } ?: "-",
                        label2 = "NP (Normalized Power)",
                        value2 = activity.npleistungWatt?.let { "$it W" } ?: "-"
                    )

                    ParameterRow(
                        label1 = "Ø Trittfrequenz",
                        value1 = activity.avgCadence?.let { "$it rpm" } ?: "-",
                        label2 = "Max. Trittfrequenz",
                        value2 = activity.maxCadence?.let { "$it rpm" } ?: "-"
                    )

                    ParameterRow(
                        label1 = "Energie",
                        value1 = activity.energieKj?.let { "$it kJ" } ?: "-",
                        label2 = "Kalorien",
                        value2 = activity.kalorienKcal?.let { "$it kcal" } ?: "-"
                    )

                    ParameterRow(
                        label1 = "Suffer Score (Strava)",
                        value1 = activity.sufferScore?.toString() ?: "-",
                        label2 = "Höhenmeter",
                        value2 = activity.hoehenmeterM?.let { "$it m" } ?: "-"
                    )

                    // Ein- und ausblendbarer Subabschnitt für Zonen-Verteilung (HF & Watt)
                    val hrDistribution = calculateEstimatedHrZones(activity, athleteProfile)
                    val powerDistribution = calculateEstimatedPowerZones(activity, athleteProfile)

                    if (hrDistribution.isNotEmpty() || powerDistribution.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { zonesExpanded = !zonesExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Zonen-Verteilung (HF & Watt)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (zonesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (zonesExpanded) "Zonen einklappen" else "Zonen ausklappen",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = zonesExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (hrDistribution.isNotEmpty()) {
                                    ZoneDistributionCard(
                                        title = "Herzfrequenz-Zonen",
                                        zones = hrDistribution,
                                        unit = "bpm"
                                    )
                                }
                                if (powerDistribution.isNotEmpty()) {
                                    ZoneDistributionCard(
                                        title = "Leistungs-Zonen (Watt)",
                                        zones = powerDistribution,
                                        unit = "W"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterRow(
    label1: String,
    value1: String,
    label2: String,
    value2: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ParameterBadge(
            label = label1,
            value = value1,
            modifier = Modifier.weight(1f)
        )
        ParameterBadge(
            label = label2,
            value = value2,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ParameterBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SyncBadge(status: SyncStatus) {
    val (color, text) = when (status) {
        SyncStatus.PENDING -> androidx.compose.ui.graphics.Color(0xFF8E24AA) to "PENDING"
        SyncStatus.SYNCED -> androidx.compose.ui.graphics.Color(0xFF2E7D32) to "SYNCED"
        SyncStatus.FAILED -> androidx.compose.ui.graphics.Color(0xFFC62828) to "FAILED"
        SyncStatus.AUTH_REQUIRED -> MaterialTheme.colorScheme.tertiary to "AUTH REQUIRED"
    }

    Surface(
        color = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Modell für ein Zonen-Element in der Verteilungsansicht
 */
data class ZoneDistributionItem(
    val zoneLabel: String,
    val name: String,
    val rangeLabel: String,
    val percent: Int,
    val seconds: Long,
    val color: Color
)

@Composable
fun ZoneDistributionCard(
    title: String,
    zones: List<ZoneDistributionItem>,
    unit: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                zones.forEach { zone ->
                    val mins = (zone.seconds + 30) / 60
                    val timeFormatted = if (mins >= 60) {
                        "%dh %02dm".format(mins / 60, mins % 60)
                    } else {
                        "%dm".format(mins)
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = zone.zoneLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = zone.color
                                )
                                Text(
                                    text = zone.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (zone.rangeLabel.isNotBlank()) {
                                    Text(
                                        text = "(${zone.rangeLabel} $unit)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${zone.percent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Fortschrittsbalken
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            val fraction = (zone.percent / 100f).coerceIn(0f, 1f)
                            if (fraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(6.dp)
                                        .background(zone.color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Berechnet die HF-Zonen-Verteilung für die Aktivität.
 * Nutzt konfigurierte Zonen aus dem Athletenprofil oder Default-Zonen (basierend auf Max-HF).
 */
fun calculateEstimatedHrZones(
    activity: ActivityEntity,
    athleteProfile: AthleteProfileEntity?
): List<ZoneDistributionItem> {
    val avgHf = activity.avgHF ?: return emptyList()
    val maxHf = activity.maxHF ?: (avgHf + 20)
    val totalSec = activity.bewegungszeitSek ?: 0L
    if (totalSec <= 0L) return emptyList()

    // 1. Zonen definieren (entweder aus Profil oder anhand Max-HF)
    val parsedZones = athleteProfile?.heartRateZonesJson?.let { parseZonesFromJson(it) } ?: emptyList()
    val ranges: List<Pair<Int, Int>> = if (parsedZones.size >= 5) {
        parsedZones.take(5)
    } else {
        val estimatedMax = maxOf(maxHf, 190)
        listOf(
            0 to (estimatedMax * 0.60).toInt(),
            (estimatedMax * 0.60).toInt() to (estimatedMax * 0.70).toInt(),
            (estimatedMax * 0.70).toInt() to (estimatedMax * 0.80).toInt(),
            (estimatedMax * 0.80).toInt() to (estimatedMax * 0.90).toInt(),
            (estimatedMax * 0.90).toInt() to (estimatedMax * 1.05).toInt()
        )
    }

    val hrColors = listOf(
        Color(0xFF9E9E9E), // Z1 Grau (Erholung)
        Color(0xFF2196F3), // Z2 Blau (Ausdauer)
        Color(0xFF4CAF50), // Z3 Grün (Tempo)
        Color(0xFFFF9800), // Z4 Orange (Schwelle)
        Color(0xFFE53935)  // Z5 Rot (Anaerob)
    )

    val hrNames = listOf("Erholung", "Ausdauer", "Tempo", "Schwelle", "Anaerob")

    // Gewichtung / Verteilungsschätzung basierend auf dem Durchschnitts-HF im Verhältnis zu den Zonen
    val weights = DoubleArray(5)
    ranges.forEachIndexed { i, range ->
        val zoneMid = if (range.second > 0) (range.first + range.second) / 2.0 else range.first + 10.0
        val diff = Math.abs(avgHf - zoneMid)
        val sigma = 14.0 // Glockenkurven-Breite
        weights[i] = Math.exp(-0.5 * Math.pow(diff / sigma, 2.0))
    }
    val sumWeights = weights.sum().coerceAtLeast(0.001)

    return ranges.mapIndexed { i, range ->
        val pct = Math.round((weights[i] / sumWeights) * 100.0).toInt().coerceIn(0, 100)
        val sec = ((pct / 100.0) * totalSec).toLong()
        val rangeLabel = if (range.second > 0 && range.second < 300) "${range.first}-${range.second}" else ">${range.first}"
        ZoneDistributionItem(
            zoneLabel = "Z${i + 1}",
            name = hrNames.getOrElse(i) { "Zone ${i + 1}" },
            rangeLabel = rangeLabel,
            percent = pct,
            seconds = sec,
            color = hrColors.getOrElse(i) { Color.Gray }
        )
    }
}

/**
 * Berechnet die Watt-Zonen-Verteilung (Coggan Z1 - Z7) für die Aktivität.
 */
fun calculateEstimatedPowerZones(
    activity: ActivityEntity,
    athleteProfile: AthleteProfileEntity?
): List<ZoneDistributionItem> {
    val avgWatt = activity.avgLeistungWatt ?: activity.npleistungWatt ?: return emptyList()
    val totalSec = activity.bewegungszeitSek ?: 0L
    if (totalSec <= 0L) return emptyList()

    val ftp = athleteProfile?.ftpWatts ?: 250 // Fallback FTP falls nicht erfasst

    val parsedZones = athleteProfile?.powerZonesJson?.let { parseZonesFromJson(it) } ?: emptyList()
    val ranges: List<Pair<Int, Int>> = if (parsedZones.size >= 5) {
        parsedZones
    } else {
        // Coggan 7-Zonen Modell
        listOf(
            0 to (ftp * 0.55).toInt(),
            (ftp * 0.55).toInt() to (ftp * 0.75).toInt(),
            (ftp * 0.75).toInt() to (ftp * 0.90).toInt(),
            (ftp * 0.90).toInt() to (ftp * 1.05).toInt(),
            (ftp * 1.05).toInt() to (ftp * 1.20).toInt(),
            (ftp * 1.20).toInt() to (ftp * 1.50).toInt(),
            (ftp * 1.50).toInt() to (ftp * 2.50).toInt()
        )
    }

    val powerColors = listOf(
        Color(0xFF9E9E9E), // Z1 Aktive Erholung
        Color(0xFF2196F3), // Z2 Grundlagenausdauer
        Color(0xFF4CAF50), // Z3 Tempo
        Color(0xFFFFB300), // Z4 Schwellenbereich
        Color(0xFFFF9800), // Z5 VO2max
        Color(0xFFE53935), // Z6 Anaerobe Kapazität
        Color(0xFF880E4F)  // Z7 Neuromuskulär
    )

    val powerNames = listOf(
        "Aktive Erholung",
        "Grundlagenausdauer",
        "Tempo",
        "Schwellenbereich",
        "VO2max",
        "Anaerobe Kapazität",
        "Neuromuskulär"
    )

    val weights = DoubleArray(ranges.size)
    ranges.forEachIndexed { i, range ->
        val zoneMid = if (range.second > 0) (range.first + range.second) / 2.0 else range.first + 20.0
        val diff = Math.abs(avgWatt - zoneMid)
        val sigma = (ftp * 0.16).coerceAtLeast(15.0)
        weights[i] = Math.exp(-0.5 * Math.pow(diff / sigma, 2.0))
    }
    val sumWeights = weights.sum().coerceAtLeast(0.001)

    return ranges.mapIndexed { i, range ->
        val pct = Math.round((weights[i] / sumWeights) * 100.0).toInt().coerceIn(0, 100)
        val sec = ((pct / 100.0) * totalSec).toLong()
        val rangeLabel = if (range.second > 0 && range.second < 2000) "${range.first}-${range.second}" else ">${range.first}"
        ZoneDistributionItem(
            zoneLabel = "Z${i + 1}",
            name = powerNames.getOrElse(i) { "Zone ${i + 1}" },
            rangeLabel = rangeLabel,
            percent = pct,
            seconds = sec,
            color = powerColors.getOrElse(i) { Color.Gray }
        )
    }
}

private fun parseZonesFromJson(json: String): List<Pair<Int, Int>> {
    return try {
        val jsonElement = Json.parseToJsonElement(json)
        if (jsonElement !is JsonArray) return emptyList()

        jsonElement.mapNotNull { element ->
            val obj = element.jsonObject
            val min = obj["min"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val max = obj["max"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
            Pair(min, max)
        }
    } catch (_: Exception) {
        emptyList()
    }
}

@Preview(showBackground = true)
@Composable
fun ActivityListContentPreview() {
    MaterialTheme {
        ActivityListContent(
            activities = listOf(
                ActivityEntity(
                    activityId = 1L,
                    startDatum = "2026-03-29",
                    startUhrzeit = "08:30:00",
                    sportart = "MountainBikeRide",
                    titel = "Sonntagsrunde im Wald",
                    istIndoor = false,
                    distanzKm = 35.5,
                    bewegungszeitSek = 5400L,
                    bewegungszeitFormatted = "01:30:00",
                    hoehenmeterM = 480,
                    gesamtzeitFormatted = "01:40:00",
                    avgGeschwindigkeitKmh = 23.6,
                    maxGeschwindigkeitKmh = 45.2,
                    avgCadence = 84,
                    maxCadence = 112,
                    avgHF = 152,
                    maxHF = 175,
                    avgLeistungWatt = 210,
                    npleistungWatt = 225,
                    energieKj = 1100,
                    kalorienKcal = 1250,
                    sufferScore = 85,
                    syncStatus = SyncStatus.SYNCED
                ),
                ActivityEntity(
                    activityId = 2L,
                    startDatum = "2026-03-28",
                    startUhrzeit = "17:15:00",
                    sportart = "Run",
                    titel = "Feierabend-Lauf",
                    istIndoor = false,
                    distanzKm = 10.2,
                    bewegungszeitSek = 3000L,
                    bewegungszeitFormatted = "00:50:00",
                    hoehenmeterM = 85,
                    gesamtzeitFormatted = "00:50:00",
                    avgGeschwindigkeitKmh = 12.24,
                    maxGeschwindigkeitKmh = 15.1,
                    avgCadence = null,
                    maxCadence = null,
                    avgHF = 162,
                    maxHF = 180,
                    avgLeistungWatt = null,
                    npleistungWatt = null,
                    energieKj = 650,
                    kalorienKcal = 720,
                    sufferScore = 60,
                    syncStatus = SyncStatus.PENDING
                )
            ),
            pendingCount = 1,
            syncedCount = 1,
            failedCount = 0
        )
    }
}
