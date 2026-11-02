package com.example.strava2sheets.ui.document

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.strava2sheets.ui.common.AppBackground
import com.example.strava2sheets.ui.dashboard.DashboardViewModel

enum class SortOrder {
    ASC, DESC
}

@Composable
fun DocumentScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetId = uiState.spreadsheetId

    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(sheetId) {
        if (sheetId.isNotBlank() && uiState.previewData.isEmpty()) {
            viewModel.loadSheetPreview()
        }
    }

    AppBackground(modifier = modifier) {
        if (sheetId.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Kein Google Sheet konfiguriert",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bitte gib im Start-Bereich eine Google Spreadsheet ID an, um das Dokument hier anzuzeigen.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            val fullEditUrl = "https://docs.google.com/spreadsheets/d/$sheetId/edit"

            Column(modifier = Modifier.fillMaxSize()) {
                // Header Action Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Google Sheet Vorschau",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { viewModel.loadSheetPreview() },
                                enabled = !uiState.isLoadingPreview
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Aktualisieren"
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullEditUrl))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Öffnen")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter Search Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Suchen / Filtern (z.B. Titel, Datum, Sportart)...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Suche") },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Löschen")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Preview Table Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        uiState.isLoadingPreview -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Lade Google Sheet Daten...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        uiState.previewErrorMessage.isNotBlank() -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Fehler beim Laden",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = uiState.previewErrorMessage,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(onClick = { viewModel.loadSheetPreview() }) {
                                        Text("Erneut versuchen")
                                    }
                                }
                            }
                        }

                        uiState.previewData.isEmpty() -> {
                            Text("Das Google Sheet ist leer oder enthält keine Daten.", style = MaterialTheme.typography.bodyMedium)
                        }

                        else -> {
                            NativeSheetTable(
                                data = uiState.previewData,
                                searchQuery = searchQuery
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NativeSheetTable(
    data: List<List<String>>,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()

    val headerRow = data.firstOrNull() ?: emptyList()
    val rawBodyRows = if (data.size > 1) data.subList(1, data.size) else emptyList()

    var sortedColumnIndex by remember { mutableIntStateOf(-1) }
    var sortOrder by remember { mutableStateOf(SortOrder.ASC) }

    // 1. Filter rows by search query
    val filteredRows = remember(rawBodyRows, searchQuery) {
        if (searchQuery.isBlank()) {
            rawBodyRows
        } else {
            val query = searchQuery.trim().lowercase()
            rawBodyRows.filter { row ->
                row.any { cell -> cell.lowercase().contains(query) }
            }
        }
    }

    // 2. Sort rows by selected column
    val displayedRows = remember(filteredRows, sortedColumnIndex, sortOrder) {
        if (sortedColumnIndex < 0) {
            filteredRows
        } else {
            filteredRows.sortedWith { rowA, rowB ->
                val valA = rowA.getOrNull(sortedColumnIndex)?.trim() ?: ""
                val valB = rowB.getOrNull(sortedColumnIndex)?.trim() ?: ""

                val numA = valA.toDoubleOrNull()
                val numB = valB.toDoubleOrNull()

                val cmp = if (numA != null && numB != null) {
                    numA.compareTo(numB)
                } else {
                    valA.compareTo(valB, ignoreCase = true)
                }

                if (sortOrder == SortOrder.ASC) cmp else -cmp
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            // Header Row with Sort Arrows
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headerRow.forEachIndexed { colIndex, title ->
                        val isSorted = colIndex == sortedColumnIndex
                        val sortIcon = if (isSorted) {
                            if (sortOrder == SortOrder.ASC) " ▲" else " ▼"
                        } else ""

                        TableCell(
                            text = "$title$sortIcon",
                            isHeader = true,
                            width = getColumnWidth(colIndex),
                            onClick = {
                                if (sortedColumnIndex == colIndex) {
                                    if (sortOrder == SortOrder.ASC) {
                                        sortOrder = SortOrder.DESC
                                    } else {
                                        sortedColumnIndex = -1
                                        sortOrder = SortOrder.ASC
                                    }
                                } else {
                                    sortedColumnIndex = colIndex
                                    sortOrder = SortOrder.ASC
                                }
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Data Rows
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(displayedRows) { rowIndex, row ->
                    val isEven = rowIndex % 2 == 0
                    val rowBackground = if (isEven) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }

                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .background(rowBackground)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headerRow.indices.forEach { colIndex ->
                            val cellValue = row.getOrNull(colIndex) ?: ""
                            TableCell(
                                text = cellValue,
                                isHeader = false,
                                width = getColumnWidth(colIndex)
                            )
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    isHeader: Boolean,
    width: Int,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .padding(horizontal = 6.dp)
            .then(if (isHeader) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = if (isHeader) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getColumnWidth(colIndex: Int): Int {
    return when (colIndex) {
        0 -> 130 // Activity ID
        1 -> 110 // Start Datum
        2 -> 100 // Start Uhrzeit
        3 -> 120 // Sportart
        4 -> 180 // Titel
        5 -> 80  // Indoor
        6 -> 120 // Distanz (km)
        7 -> 130 // Bewegungszeit (s)
        8 -> 130 // Bewegungszeit (HH:MM:SS)
        9 -> 110 // Höhenmeter (m)
        10 -> 130 // Gesamtzeit (HH:MM:SS)
        11 -> 130 // Avg Geschwindigkeit
        12 -> 130 // Max Geschwindigkeit
        13 -> 110 // Avg Cadence
        14 -> 110 // Max Cadence
        15 -> 100 // Avg HF
        16 -> 100 // Max HF
        17 -> 120 // Avg Leistung
        18 -> 120 // NP Leistung
        19 -> 110 // Energie (kJ)
        20 -> 120 // Kalorien (kcal)
        21 -> 110 // Suffer Score
        else -> 110
    }
}
