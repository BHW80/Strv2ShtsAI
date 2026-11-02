package com.example.strava2sheets.ui.athlete

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.strava2sheets.data.local.AthleteProfileEntity
import com.example.strava2sheets.ui.common.AppBackground
import com.example.strava2sheets.ui.dashboard.DashboardViewModel
import com.example.strava2sheets.ui.fitness.DailyFitnessPoint
import com.example.strava2sheets.ui.fitness.FitnessEngine
import com.example.strava2sheets.ui.fitness.FitnessTimeSpan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.format.DateTimeFormatter

enum class AthleteTab(val label: String) {
    OVERVIEW("Profil & Zonen"),
    PROGRESSION("Gewicht und Watt"),
    FITNESS("Fitness/Form")
}

@Composable
fun AthleteScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(AthleteTab.OVERVIEW) }
    var selectedTimeSpan by remember { mutableStateOf(FitnessTimeSpan.LAST_6_MONTHS) }

    // Fitness-Berechnung basierend auf lokalen Aktivitäten, gewählter Zeitspanne und FTP
    val fitnessPoints = remember(uiState.activities, uiState.latestAthleteProfile, selectedTimeSpan) {
        FitnessEngine.calculateFitnessHistory(
            activities = uiState.activities,
            ftp = uiState.latestAthleteProfile?.ftpWatts,
            daysToInclude = selectedTimeSpan.days
        )
    }

    AppBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Benutzer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = { viewModel.fetchAthleteProfile() },
                    enabled = !uiState.isFetchingAthlete && uiState.isStravaConnected
                ) {
                    if (uiState.isFetchingAthlete) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Aktualisieren")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-Navigation Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AthleteTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    AthleteTab.OVERVIEW -> {
                        AthleteOverviewTab(profile = uiState.latestAthleteProfile)
                    }
                    AthleteTab.PROGRESSION -> {
                        AthleteProgressionTab(history = uiState.athleteProfilesHistory)
                    }
                    AthleteTab.FITNESS -> {
                        AthleteFitnessTab(
                            fitnessPoints = fitnessPoints,
                            selectedTimeSpan = selectedTimeSpan,
                            onSelectTimeSpan = { selectedTimeSpan = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AthleteOverviewTab(profile: AthleteProfileEntity?) {
    if (profile == null) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "👤", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Noch keine Nutzerdaten geladen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Klicke oben auf 'Aktualisieren', um deine aktuellen Daten (Gewicht, FTP, Herzfrequenz- & Watt-Zonen) von Strava zu laden.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Profil Header Card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                val fullName = "${profile.firstname ?: ""} ${profile.lastname ?: ""}".trim().ifBlank { "Athlet" }
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Stand: ${profile.date} • ID: ${profile.athleteId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }

    // Key Stats Grid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Gewicht",
            value = profile.weightKg?.let { "%.1f kg".format(it) } ?: "-",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "FTP",
            value = profile.ftpWatts?.let { "$it W" } ?: "-",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "W / kg",
            value = profile.calculatedWPerKg?.let { "%.2f".format(it) } ?: "-",
            highlight = true,
            modifier = Modifier.weight(1f)
        )
    }

    // HF Zonen Visualisierung
    profile.heartRateZonesJson?.let { json ->
        val hrZones = parseZonesFromJson(json)
        if (hrZones.isNotEmpty()) {
            ZoneCard(
                title = "Herzfrequenz-Zonen (bpm)",
                zones = hrZones,
                unit = "bpm"
            )
        }
    }

    // Power Zonen Visualisierung
    profile.powerZonesJson?.let { json ->
        val powerZones = parseZonesFromJson(json)
        if (powerZones.isNotEmpty()) {
            ZoneCard(
                title = "Leistungs-Zonen (Watt - Coggan)",
                zones = powerZones,
                unit = "W"
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ZoneCard(
    title: String,
    zones: List<Pair<Int, Int>>,
    unit: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            val zoneColors = listOf(
                Color(0xFF64B5F6), // Z1 Erholung (Blau)
                Color(0xFF81C784), // Z2 Ausdauer (Grün)
                Color(0xFFFFD54F), // Z3 Tempo (Gelb)
                Color(0xFFFF8A65), // Z4 Schwelle (Orange)
                Color(0xFFE57373), // Z5 VO2Max (Rot)
                Color(0xFFBA68C8), // Z6 Anaerob (Lila)
                Color(0xFF90A4AE)  // Z7 Neuromuskulär (Grau)
            )

            zones.forEachIndexed { index, (min, max) ->
                val color = zoneColors.getOrElse(index) { Color.Gray }
                val maxLabel = if (max == -1 || max > 1000) "max" else "$max $unit"
                val minLabel = if (min <= 0) "0" else "$min $unit"

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Zone ${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "$minLabel - $maxLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AthleteProgressionTab(history: List<AthleteProfileEntity>) {
    // Sortierung: Neueste oben (absteigend nach Datum)
    val sortedHistory = remember(history) {
        history.sortedByDescending { it.date }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Historischer Verlauf",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${sortedHistory.size} gespeicherte Tage erfasst",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (sortedHistory.isEmpty()) {
                Text(
                    text = "Noch keine Historie vorhanden. Aktualisiere deine Daten regelmäßig, um den Fortschritt zu tracken.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Verlaufsgrafik für Gewicht und Watt (in chronologischer Reihenfolge von links nach rechts)
                val chronologicalHistory = remember(sortedHistory) { sortedHistory.reversed() }
                if (chronologicalHistory.size >= 2) {
                    Text(
                        text = "Entwicklung",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendItem(label = "Gewicht (kg)", color = Color(0xFF4CAF50))
                        LegendItem(label = "FTP (Watt)", color = Color(0xFF2196F3))
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val weightColor = Color(0xFF4CAF50)
                        val ftpColor = Color(0xFF2196F3)

                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp)) {
                            val w = size.width
                            val h = size.height

                            val weightPoints = chronologicalHistory.mapNotNull { it.weightKg?.toFloat() }
                            val ftpPoints = chronologicalHistory.mapNotNull { it.ftpWatts?.toFloat() }

                            // 1. Gewichtskurve (grün)
                            if (weightPoints.size >= 2) {
                                val minW = (weightPoints.minOrNull() ?: 60f) * 0.95f
                                val maxW = (weightPoints.maxOrNull() ?: 90f) * 1.05f
                                val rangeW = (maxW - minW).coerceAtLeast(1f)

                                val wPath = Path()
                                chronologicalHistory.forEachIndexed { i, item ->
                                    val x = (i.toFloat() / (chronologicalHistory.size - 1).toFloat()) * w
                                    val wVal = item.weightKg?.toFloat()
                                    if (wVal != null) {
                                        val y = h - ((wVal - minW) / rangeW * h).coerceIn(0f, h)
                                        if (wPath.isEmpty) wPath.moveTo(x, y) else wPath.lineTo(x, y)
                                        drawCircle(color = weightColor, radius = 3.dp.toPx(), center = Offset(x, y))
                                    }
                                }
                                drawPath(path = wPath, color = weightColor, style = Stroke(width = 2.5.dp.toPx()))
                            }

                            // 2. Wattkurve (blau)
                            if (ftpPoints.size >= 2) {
                                val minF = (ftpPoints.minOrNull() ?: 150f) * 0.9f
                                val maxF = (ftpPoints.maxOrNull() ?: 300f) * 1.1f
                                val rangeF = (maxF - minF).coerceAtLeast(10f)

                                val fPath = Path()
                                chronologicalHistory.forEachIndexed { i, item ->
                                    val x = (i.toFloat() / (chronologicalHistory.size - 1).toFloat()) * w
                                    val fVal = item.ftpWatts?.toFloat()
                                    if (fVal != null) {
                                        val y = h - ((fVal - minF) / rangeF * h).coerceIn(0f, h)
                                        if (fPath.isEmpty) fPath.moveTo(x, y) else fPath.lineTo(x, y)
                                        drawCircle(color = ftpColor, radius = 3.dp.toPx(), center = Offset(x, y))
                                    }
                                }
                                drawPath(path = fPath, color = ftpColor, style = Stroke(width = 2.5.dp.toPx()))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Einträge sortiert: Neueste oben
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedHistory.take(20).forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = item.date, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(text = item.weightKg?.let { "%.1f kg".format(it) } ?: "-")
                                    Text(text = item.ftpWatts?.let { "$it W" } ?: "-")
                                    Text(
                                        text = item.calculatedWPerKg?.let { "%.2f W/kg".format(it) } ?: "-",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
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

/**
 * Ausführlicher Tab für Fitness, Fatigue & Form (Banister Modell)
 * mit Zeitspannen-Auswahl und detaillierter Achsen- & Kennzahlen-Beschriftung.
 */
@Composable
fun AthleteFitnessTab(
    fitnessPoints: List<DailyFitnessPoint>,
    selectedTimeSpan: FitnessTimeSpan,
    onSelectTimeSpan: (FitnessTimeSpan) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Fitness & Form (Banister Modell)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Performance Management Chart (CTL / ATL / TSB)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Zeitspannen-Auswahl (Horizontale Scrollzeile)
            val timeScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(timeScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FitnessTimeSpan.entries.forEach { span ->
                    FilterChip(
                        selected = selectedTimeSpan == span,
                        onClick = { onSelectTimeSpan(span) },
                        label = { Text(span.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val latest = fitnessPoints.lastOrNull()
            if (latest != null) {
                // Aktuelle Kennzahlen-Karten (Fitness, Ermüdung, Form)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricBox(
                        title = "Fitness (CTL)",
                        subtitle = "42d Schnitt",
                        value = latest.fitness.toString(),
                        color = Color(0xFF1E88E5),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Ermüdung (ATL)",
                        subtitle = "7d Schnitt",
                        value = latest.fatigue.toString(),
                        color = Color(0xFFE53935),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Form (TSB)",
                        subtitle = "CTL - ATL",
                        value = "${if (latest.form >= 0) "+" else ""}${latest.form}",
                        color = if (latest.form >= 0) Color(0xFF43A047) else Color(0xFFFB8C00),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Titel
                Text(
                    text = "Verlauf (${fitnessPoints.size} Tage)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Legende horizontal scrollbar, damit sie auf schmalen Bildschirmen niemals vertikal umbricht
                val legendScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(legendScrollState),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(label = "Fitness (CTL)", color = Color(0xFF1E88E5))
                    LegendItem(label = "Ermüdung (ATL)", color = Color(0xFFE53935))
                    LegendItem(label = "Form (TSB)", color = Color(0xFF43A047))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chart Canvas mit Nulllinie, Achsen und Kurven
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val ctlColor = Color(0xFF1E88E5)
                    val atlColor = Color(0xFFE53935)
                    val tsbColor = Color(0xFF43A047)
                    val zeroLineColor = Color.Gray.copy(alpha = 0.4f)

                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp)) {
                        val w = size.width
                        val h = size.height

                        if (fitnessPoints.size < 2) return@Canvas

                        // Wertebereich ermitteln (inklusive negativer TSB Werte)
                        val maxPositive = fitnessPoints.maxOf { maxOf(it.fitness, it.fatigue, it.form.coerceAtLeast(0.0)) }
                        val minNegative = fitnessPoints.minOf { it.form.coerceAtMost(0.0) }

                        val topVal = (maxPositive * 1.15).coerceAtLeast(20.0).toFloat()
                        val bottomVal = (minNegative * 1.15).coerceAtMost(-10.0).toFloat()
                        val totalRange = topVal - bottomVal

                        // Y Position für Wert 0.0
                        val yZero = h - ((0f - bottomVal) / totalRange * h)

                        // Nulllinie zeichnen
                        drawLine(
                            color = zeroLineColor,
                            start = Offset(0f, yZero),
                            end = Offset(w, yZero),
                            strokeWidth = 1.dp.toPx()
                        )

                        val ctlPath = Path()
                        val atlPath = Path()
                        val tsbPath = Path()

                        fitnessPoints.forEachIndexed { i, pt ->
                            val x = (i.toFloat() / (fitnessPoints.size - 1).toFloat()) * w
                            val yCtl = h - ((pt.fitness.toFloat() - bottomVal) / totalRange * h).coerceIn(0f, h)
                            val yAtl = h - ((pt.fatigue.toFloat() - bottomVal) / totalRange * h).coerceIn(0f, h)
                            val yTsb = h - ((pt.form.toFloat() - bottomVal) / totalRange * h).coerceIn(0f, h)

                            if (i == 0) {
                                ctlPath.moveTo(x, yCtl)
                                atlPath.moveTo(x, yAtl)
                                tsbPath.moveTo(x, yTsb)
                            } else {
                                ctlPath.lineTo(x, yCtl)
                                atlPath.lineTo(x, yAtl)
                                tsbPath.lineTo(x, yTsb)
                            }
                        }

                        // Form (TSB) Kurve (dünner)
                        drawPath(path = tsbPath, color = tsbColor.copy(alpha = 0.85f), style = Stroke(width = 1.5.dp.toPx()))

                        // Ermüdung (ATL) Kurve
                        drawPath(path = atlPath, color = atlColor.copy(alpha = 0.8f), style = Stroke(width = 2.dp.toPx()))

                        // Fitness (CTL) Kurve (stärker im Vordergrund)
                        drawPath(path = ctlPath, color = ctlColor, style = Stroke(width = 3.dp.toPx()))
                    }
                }

                // Datums-Labels an der Zeitachse (Start, Mitte, Ende)
                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(fitnessPoints.first().date.format(dateFormatter), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(fitnessPoints[fitnessPoints.size / 2].date.format(dateFormatter), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(fitnessPoints.last().date.format(dateFormatter), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Erklärung / Formel-Legende
                Text(
                    text = "Interpretation der Werte:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Fitness (CTL): Bildet die Trainingsbelastung der letzten 42 Tage ab (langfristige Ausdauerbasis).\n" +
                            "• Ermüdung (ATL): Reagiert schnell auf die Belastung der letzten 7 Tage.\n" +
                            "• Form (TSB = CTL - ATL): Positive Form (> 0) bedeutet Frische für Wettkämpfe; negative Form (< -15) deutet auf hohe Ermüdung oder Überlastung hin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Keine Aktivitätsdaten für den ausgewählten Zeitraum gefunden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    subtitle: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(10.5f, androidx.compose.ui.unit.TextUnitType.Sp)),
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(9.5f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp, 8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            maxLines = 1,
            softWrap = false
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
