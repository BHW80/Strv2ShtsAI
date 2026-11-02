package com.example.strava2sheets.ui.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.strava2sheets.data.local.GoalEntity
import com.example.strava2sheets.data.local.GoalMetricType
import com.example.strava2sheets.data.local.GoalPeriodType
import com.example.strava2sheets.data.local.SeasonalityCurve
import com.example.strava2sheets.ui.common.AppBackground
import com.example.strava2sheets.ui.dashboard.DashboardViewModel
import java.time.LocalDate

@Composable
fun GoalsScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }

    // Ausklappbare Ziele (Standard eingeklappt, Mehrfachauswahl erlaubt)
    val expandedGoalIds = remember { mutableStateListOf<Long>() }

    val goalProgresses = remember(uiState.goals, uiState.activities) {
        uiState.goals.map { goal ->
            GoalCalculator.calculateProgress(goal, uiState.activities)
        }
    }

    AppBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ziele (${uiState.goals.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.goals.isNotEmpty()) {
                    Button(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ziel hinzufügen")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.goals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎯",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Noch keine Ziele definiert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Definiere individuelle Jahres-, Monats- oder Wochenziele für Rudern, Radfahren, Laufen und verfolge deinen Fortschritt mit intelligenten Saisonalitätskurven.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ziel hinzufügen")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(goalProgresses, key = { it.goal.id }) { progress ->
                        val isExpanded = expandedGoalIds.contains(progress.goal.id)
                        GoalCard(
                            progress = progress,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                if (isExpanded) {
                                    expandedGoalIds.remove(progress.goal.id)
                                } else {
                                    expandedGoalIds.add(progress.goal.id)
                                }
                            },
                            onEdit = { goalToEdit = progress.goal },
                            onDelete = { goalToDelete = progress.goal }
                        )
                    }
                }
            }
        }
    }

    // Verfügbare Sportarten aus Aktivitäten und Defaults
    val availableSports = remember(uiState.activities) {
        val sportsFromActivities = uiState.activities.map { it.sportart.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        val defaultSports = listOf("Ride", "MountainBikeRide", "GravelRide", "VirtualRide", "Rowing", "VirtualRow", "Run", "Walk")
        val combined = (listOf("Alle Sportarten") + sportsFromActivities + defaultSports).distinct()
        combined
    }

    // Add Goal Dialog
    if (showAddDialog) {
        GoalDialog(
            goal = null,
            availableSports = availableSports,
            onDismiss = { showAddDialog = false },
            onConfirm = { newGoal ->
                viewModel.addGoal(newGoal)
                showAddDialog = false
            }
        )
    }

    // Edit Goal Dialog
    if (goalToEdit != null) {
        GoalDialog(
            goal = goalToEdit,
            availableSports = availableSports,
            onDismiss = { goalToEdit = null },
            onConfirm = { updatedGoal ->
                viewModel.updateGoal(updatedGoal)
                goalToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Ziel löschen") },
            text = { Text("Möchtest du das Ziel '${goalToDelete?.title}' wirklich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        goalToDelete?.let { viewModel.deleteGoal(it) }
                        goalToDelete = null
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { goalToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun GoalCard(
    progress: GoalProgress,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = progress.goal
    val progressClamped = (progress.progressPercent / 100.0).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row (Clickable zum Auf-/Zuklappen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${goal.sportart ?: "Alle Sportarten"} • ${when (goal.periodType) {
                            GoalPeriodType.YEARLY -> "Jahresziel ${goal.year}"
                            GoalPeriodType.MONTHLY -> "Monatsziel"
                            GoalPeriodType.WEEKLY -> "Wochenziel"
                        }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit Button
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Ziel bearbeiten",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete Button
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Ziel löschen",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Expand / Collapse Chevron
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Einklappen" else "Ausklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kompakte Ansicht: Werte & Fortschrittsbalken immer sichtbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = progress.formattedCurrent,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / ${progress.formattedTarget}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                    )
                }

                Text(
                    text = "%.1f%%".format(progress.progressPercent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressClamped },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // Ausklappbarer Bereich (Details, Soll/Ist Status und Saisonalitätsgraph)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Schedule & Seasonality Status
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val statusText = if (progress.isAheadOfSchedule) {
                                    "Im Soll / Vorsprung: ${progress.formattedDelta}"
                                } else {
                                    "Rückstand: ${progress.formattedDelta}"
                                }
                                val statusColor = if (progress.isAheadOfSchedule) Color(0xFF2E7D32) else Color(0xFFC62828)

                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )

                                if (goal.periodType == GoalPeriodType.YEARLY) {
                                    Text(
                                        text = "Modell: ${goal.seasonality.label.substringBefore(" (")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "Noch ${progress.remainingDays} Tage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Feature: Jahresziel Saisonalitäts-Kurve (Soll vs. Ist Graph)
                    if (goal.periodType == GoalPeriodType.YEARLY && progress.chartPoints.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SeasonalityCurveChart(
                            points = progress.chartPoints,
                            seasonality = goal.seasonality
                        )
                    }
                }
            }
        }
    }
}

/**
 * Zeichnet die Saisonalitätskurve:
 * - Gestrichelte/helle Soll-Kurve (Sigmoid / S-Kurve oder Linear) über das gesamte Jahr
 * - Kräftige durchgezogene Ist-Kurve darübergelegt bis zum heutigen Tag
 */
@Composable
fun SeasonalityCurveChart(
    points: List<CurvePoint>,
    seasonality: SeasonalityCurve,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saisonverlauf (${seasonality.label.substringBefore(" (")})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Legende Soll
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp, 2.dp).background(Color.Gray))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Soll", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                // Legende Ist
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp, 2.dp).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Ist", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val expectedColor = Color.Gray.copy(alpha = 0.8f)

            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp)) {
                val w = size.width
                val h = size.height

                if (points.size < 2) return@Canvas

                // 1. Soll-Kurve zeichnen (Gestrichelt)
                val expectedPath = Path()
                points.forEachIndexed { index, pt ->
                    val x = (pt.dayOfYear.toFloat() / 365f) * w
                    val y = h - (pt.expectedRatio.coerceIn(0f, 1f) * h)
                    if (index == 0) expectedPath.moveTo(x, y) else expectedPath.lineTo(x, y)
                }

                drawPath(
                    path = expectedPath,
                    color = expectedColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )

                // 2. Ist-Kurve zeichnen (Kräftig durchgezogen bis heute)
                val actualPoints = points.filter { it.actualRatio != null }
                if (actualPoints.isNotEmpty()) {
                    val actualPath = Path()
                    var lastX = 0f
                    var lastY = h

                    actualPoints.forEachIndexed { index, pt ->
                        val ratio = pt.actualRatio ?: 0f
                        val x = (pt.dayOfYear.toFloat() / 365f) * w
                        val y = h - (ratio.coerceIn(0f, 1.2f) * h)
                        lastX = x
                        lastY = y
                        if (index == 0) actualPath.moveTo(x, y) else actualPath.lineTo(x, y)
                    }

                    drawPath(
                        path = actualPath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Punkt am aktuellen Stand
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
            }
        }

        // Monats-Labels unten (Q1 bis Q4)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Jan", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Apr", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Jul", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Okt", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Dez", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

/**
 * Universeller Dialog zum Hinzufügen ODER Bearbeiten eines Ziels.
 */
@Composable
fun GoalDialog(
    goal: GoalEntity?,
    availableSports: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (GoalEntity) -> Unit
) {
    val isEditing = goal != null
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var selectedSport by remember { mutableStateOf(goal?.sportart ?: "Alle Sportarten") }
    var selectedMetric by remember { mutableStateOf(goal?.metricType ?: GoalMetricType.DISTANCE) }
    var targetInput by remember { mutableStateOf(goal?.targetValue?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "1000") }
    var selectedPeriod by remember { mutableStateOf(goal?.periodType ?: GoalPeriodType.YEARLY) }
    var selectedSeasonality by remember { mutableStateOf(goal?.seasonality ?: SeasonalityCurve.LINEAR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Ziel bearbeiten" else "Neues Ziel hinzufügen", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Ziel-Titel (z.B. Rennrad Sommer 2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Zeitraum
                Text("Zeitraum:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalPeriodType.entries.forEach { p ->
                        FilterChip(
                            selected = selectedPeriod == p,
                            onClick = { selectedPeriod = p },
                            label = {
                                Text(when (p) {
                                    GoalPeriodType.WEEKLY -> "Woche"
                                    GoalPeriodType.MONTHLY -> "Monat"
                                    GoalPeriodType.YEARLY -> "Jahr"
                                })
                            }
                        )
                    }
                }

                // Kennzahl / Metrik (Hm)
                Text("Kennzahl:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GoalMetricType.entries.take(3).forEach { m ->
                        FilterChip(
                            selected = selectedMetric == m,
                            onClick = { selectedMetric = m },
                            label = { Text(m.label) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GoalMetricType.entries.drop(3).forEach { m ->
                        FilterChip(
                            selected = selectedMetric == m,
                            onClick = { selectedMetric = m },
                            label = { Text(m.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Zielwert (${selectedMetric.unit})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Sportart: Horizontale Scrollzeile für alle verfügbaren Sportarten
                Text("Sportart:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                val sportScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(sportScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableSports.forEach { s ->
                        FilterChip(
                            selected = selectedSport.equals(s, ignoreCase = true),
                            onClick = { selectedSport = s },
                            label = { Text(s) }
                        )
                    }
                }

                // Saisonalität (nur bei Jahresziel sichtbar)
                if (selectedPeriod == GoalPeriodType.YEARLY) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Saisonalitäts-Kurve (Jahresziel):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    SeasonalityCurve.entries.forEach { curve ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedSeasonality == curve,
                                onClick = { selectedSeasonality = curve },
                                label = { Text(curve.label, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTarget = targetInput.trim().replace(',', '.').toDoubleOrNull() ?: 100.0
                    val finalTitle = title.ifBlank {
                        "${selectedMetric.label} ${if (selectedSport != "Alle Sportarten") selectedSport else ""}".trim()
                    }
                    val updatedOrNewGoal = goal?.copy(
                        title = finalTitle,
                        sportart = if (selectedSport == "Alle Sportarten") null else selectedSport.trim(),
                        metricType = selectedMetric,
                        targetValue = parsedTarget,
                        periodType = selectedPeriod,
                        seasonality = selectedSeasonality
                    ) ?: GoalEntity(
                        title = finalTitle,
                        sportart = if (selectedSport == "Alle Sportarten") null else selectedSport.trim(),
                        metricType = selectedMetric,
                        targetValue = parsedTarget,
                        periodType = selectedPeriod,
                        year = LocalDate.now().year,
                        seasonality = selectedSeasonality
                    )
                    onConfirm(updatedOrNewGoal)
                }
            ) {
                Text(if (isEditing) "Aktualisieren" else "Speichern")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
