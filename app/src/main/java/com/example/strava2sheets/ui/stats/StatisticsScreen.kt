package com.example.strava2sheets.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.strava2sheets.ui.common.AppBackground
import com.example.strava2sheets.ui.dashboard.DashboardViewModel

@Composable
fun StatisticsScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedRange by rememberSaveable { mutableStateOf(StatsTimeRange.THIS_MONTH) }
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedSubView by rememberSaveable { mutableStateOf(YearSubView.OVERVIEW) }
    var selectedSport by rememberSaveable { mutableStateOf<String?>(null) }

    val statsState = remember(uiState.activities, selectedRange, selectedYear, selectedSubView, selectedSport) {
        StatisticsEngine.calculate(
            activities = uiState.activities,
            timeRange = selectedRange,
            selectedYear = selectedYear,
            selectedYearSubView = selectedSubView,
            selectedSport = selectedSport
        )
    }

    StatisticsContent(
        state = statsState,
        onSelectRange = { selectedRange = it },
        onSelectYear = { selectedYear = it },
        onSelectSubView = { selectedSubView = it },
        onSelectSport = { selectedSport = it },
        modifier = modifier
    )
}

@Composable
fun StatisticsContent(
    state: StatisticsUiState,
    onSelectRange: (StatsTimeRange) -> Unit,
    onSelectYear: (Int) -> Unit,
    onSelectSubView: (YearSubView) -> Unit,
    onSelectSport: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterExpanded by rememberSaveable { mutableStateOf(true) }

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
                    text = "Leistungs-Statistik",
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

                    // 1. Time Range Chips Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(StatsTimeRange.entries) { range ->
                            FilterChip(
                                selected = state.selectedTimeRange == range,
                                onClick = { onSelectRange(range) },
                                label = { Text(range.label) }
                            )
                        }
                    }

                    // 2. Year Selector Chips Row (Visible when "Nach Jahr" is selected)
                    AnimatedVisibility(
                        visible = state.selectedTimeRange == StatsTimeRange.BY_YEAR && state.availableYears.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Jahr auswählen",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 4.dp)
                                    )
                                }
                                items(state.availableYears) { year ->
                                    FilterChip(
                                        selected = state.selectedYear == year,
                                        onClick = { onSelectYear(year) },
                                        label = { Text(year.toString()) }
                                    )
                                }
                            }

                            // 3. Sub-View Chips Row: Aufspringen bei gewähltem Jahr
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(YearSubView.entries) { subView ->
                                    FilterChip(
                                        selected = state.selectedYearSubView == subView,
                                        onClick = { onSelectSubView(subView) },
                                        label = { Text(subView.label) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 4. Sport Filter Chips Row (if multiple sports exist)
                    if (state.availableSports.size > 1) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedSport == null,
                                    onClick = { onSelectSport(null) },
                                    label = { Text("Alle Sportarten") }
                                )
                            }
                            items(state.availableSports) { sport ->
                                FilterChip(
                                    selected = state.selectedSport == sport,
                                    onClick = { onSelectSport(sport) },
                                    label = { Text(sport) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val summary = state.summary

            if (summary == null || summary.workoutCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "Keine Aktivitäten im gewählten Zeitraum gefunden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Unterschiedliche Ansichten je nach gewähltem Modus
                if (state.selectedTimeRange == StatsTimeRange.BY_YEAR) {
                    when (state.selectedYearSubView) {
                        YearSubView.MONTHLY -> {
                            PivotTableView(
                                title = "Monatsübersicht ${state.selectedYear}",
                                items = state.monthlyPivots,
                                total = state.pivotTotal,
                                periodHeaderName = "Monat"
                            )
                        }
                        YearSubView.WEEKLY -> {
                            PivotTableView(
                                title = "Wochenübersicht ${state.selectedYear}",
                                items = state.weeklyPivots,
                                total = state.pivotTotal,
                                periodHeaderName = "Woche"
                            )
                        }
                        YearSubView.PROGRESSION -> {
                            state.ytdProgression?.let { prog ->
                                ProgressionComparisonView(progression = prog)
                            }
                        }
                        YearSubView.SPORTS -> {
                            SportsOnlyView(breakdowns = state.sportBreakdowns)
                        }
                        YearSubView.OVERVIEW -> {
                            StandardOverviewView(state = state, summary = summary)
                        }
                    }
                } else {
                    StandardOverviewView(state = state, summary = summary)
                }
            }
        }
    }
}

/**
 * Universelle Pivot-Tabelle für Monats- oder Wochenauswertung mit Kennzahlen:
 * Distanz (km), Höhenmeter (m), Netto-Zeit (HH:MM:SS), Kalorien (kcal) + Gesamtsumme
 */
@Composable
fun PivotTableView(
    title: String,
    items: List<PivotItem>,
    total: PivotItem?,
    periodHeaderName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            val scrollState = rememberScrollState()

            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                Column {
                    // Header Row
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = periodHeaderName, modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(text = "Workouts", modifier = Modifier.width(75.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                            Text(text = "Distanz", modifier = Modifier.width(95.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                            Text(text = "Höhenm.", modifier = Modifier.width(85.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                            Text(text = "Netto-Zeit", modifier = Modifier.width(95.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                            Text(text = "Kalorien", modifier = Modifier.width(85.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Data Rows
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(items) { item ->
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = item.periodLabel, modifier = Modifier.width(90.dp), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Text(text = "${item.count}", modifier = Modifier.width(75.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                                Text(text = "%.1f km".format(item.distanceKm), modifier = Modifier.width(95.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                                Text(text = "${item.elevationM} m", modifier = Modifier.width(85.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                                Text(text = item.timeFormatted, modifier = Modifier.width(95.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                                Text(text = "${item.caloriesKcal}", modifier = Modifier.width(85.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        }

                        // Total Footer Row
                        if (total != null) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "SUMME", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                        Text(text = "${total.count}", modifier = Modifier.width(75.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                                        Text(text = "%.1f km".format(total.distanceKm), modifier = Modifier.width(95.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                                        Text(text = "${total.elevationM} m", modifier = Modifier.width(85.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                                        Text(text = total.timeFormatted, modifier = Modifier.width(95.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                                        Text(text = "${total.caloriesKcal}", modifier = Modifier.width(85.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
                                    }
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
 * Progression YTD: Vergleich zwischen dem aktuellen Kalenderjahr und dem ausgewählten Jahr (Year-to-Date bis heute)
 */
@Composable
fun ProgressionComparisonView(progression: YtdProgressionComparison) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progression Year-to-Date (Stand ${progression.asOfDateFormatted})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vergleich des aktuellen Jahres ${progression.currentYear} mit dem Jahr ${progression.selectedYear} exakt bis zum heutigen Kalendertag.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Vergleichs-Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kennzahl",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "${progression.currentYear} (Aktuell)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "${progression.selectedYear}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "Trend",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.9f),
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            progression.metrics.forEach { metric ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = metric.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = metric.currentYearValue,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = metric.selectedYearValue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )

                    val trend = metric.diffPercent
                    val trendText = if (trend != null) {
                        "${if (trend >= 0) "+" else ""}${"%.1f".format(trend)}%"
                    } else "-"
                    val trendColor = if (trend == null) Color.Gray else if (trend >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)

                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = trendColor,
                        modifier = Modifier.weight(0.9f),
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.25f))
            }
        }
    }
}

@Composable
fun SportsOnlyView(breakdowns: List<SportBreakdown>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Sportarten-Verteilung im Jahresverlauf",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(breakdowns) { item ->
            SportBreakdownRow(item = item)
        }
    }
}

@Composable
fun StandardOverviewView(state: StatisticsUiState, summary: PeriodSummary) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: KPI Summary Cards with Period Trends
        item {
            val trendLabel = when (state.selectedTimeRange) {
                StatsTimeRange.BY_YEAR -> "Jahres-Übersicht & Vergleich zum Vorjahr (${state.selectedYear?.minus(1)})"
                StatsTimeRange.ALL_TIME -> "Gesamt-Übersicht aller Zeiten"
                else -> "Übersicht & Trends vs. Vorperiode"
            }

            Text(
                text = trendLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMetricCard(
                    label = "Trainingszeit",
                    value = summary.timeTrend.formattedCurrent,
                    trend = summary.timeTrend.percentChange,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "Distanz",
                    value = summary.distanceTrend.formattedCurrent,
                    trend = summary.distanceTrend.percentChange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMetricCard(
                    label = "Kalorien",
                    value = summary.caloriesTrend.formattedCurrent,
                    trend = summary.caloriesTrend.percentChange,
                    modifier = Modifier.weight(1f)
                )
                HeroMetricCard(
                    label = "Einheiten",
                    value = summary.countTrend.formattedCurrent,
                    trend = summary.countTrend.percentChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section 2: Indoor / Ergometer vs. Outdoor Split
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trainingsumgebung",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Indoor (Ergometer/Ruder): ${summary.indoorPercentage}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Outdoor: ${summary.outdoorPercentage}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { summary.indoorPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Section 3: Performance Parameters (HF, Watts, Cadence/SPM)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Intensität & Dynamik im Zeitraum",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricValueBlock(
                            title = "Ø Herzfrequenz",
                            value = summary.avgHeartRate?.let { "$it bpm" } ?: "-"
                        )
                        MetricValueBlock(
                            title = "Ø Leistung",
                            value = summary.avgWatts?.let { "$it W" } ?: "-"
                        )
                        MetricValueBlock(
                            title = "Ø Kadenz/SPM",
                            value = summary.avgCadence?.let { "$it" } ?: "-"
                        )
                        MetricValueBlock(
                            title = "Max Kadenz/SPM",
                            value = summary.maxCadence?.let { "$it" } ?: "-"
                        )
                    }
                }
            }
        }

        // Section 4: Sport Breakdown
        if (state.sportBreakdowns.isNotEmpty()) {
            item {
                Text(
                    text = "Sportarten-Verteilung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(state.sportBreakdowns) { item ->
                SportBreakdownRow(item = item)
            }
        }

        // Section 5: Personal Bests Wall ("Hall of Fame")
        if (state.personalRecords.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rekorde",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.selectedTimeRange == StatsTimeRange.BY_YEAR) {
                            "Jahres-Bestleistungen (${state.selectedYear})"
                        } else {
                            "Persönliche Bestleistungen (Hall of Fame)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(state.personalRecords) { record ->
                PersonalRecordCard(record = record)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HeroMetricCard(
    label: String,
    value: String,
    trend: Double?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (trend != null) {
                val isPositive = trend >= 0
                val trendText = "${if (isPositive) "▲ +" else "▼ "}${"%.1f".format(trend)}%"
                val trendColor = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)

                Text(
                    text = trendText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
            } else {
                Text(
                    text = "Keine Vorperiode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MetricValueBlock(
    title: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SportBreakdownRow(item: SportBreakdown) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.sport} (${item.count}x)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.totalDurationFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { item.percentageOfTime },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            if (item.totalDistanceKm > 0.0 || item.maxCadenceOrSpm != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (item.totalDistanceKm > 0.0) {
                        Text(
                            text = "Distanz: ${"%.1f km".format(item.totalDistanceKm)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.maxCadenceOrSpm != null) {
                        Text(
                            text = "Peak SPM/RPM: ${item.maxCadenceOrSpm}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalRecordCard(record: PersonalRecordItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏆", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = record.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${record.sport} • ${record.activityTitle} (${record.activityDate})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
