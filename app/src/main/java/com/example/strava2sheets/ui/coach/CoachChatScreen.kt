package com.example.strava2sheets.ui.coach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.strava2sheets.ai.ChatMessage
import com.example.strava2sheets.ui.common.AppBackground

@Composable
fun CoachChatScreen(
    viewModel: CoachChatViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Auto-scroll ans Ende bei neuen Nachrichten oder während des Streamings
    val totalItemCount = uiState.messages.size + if (uiState.isStreaming) 1 else 0
    LaunchedEffect(totalItemCount, uiState.currentStreamingResponse) {
        if (totalItemCount > 0) {
            listState.animateScrollToItem(totalItemCount - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        AppBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header-Bar mit AeroCoach Titel & API-Key Button
                CoachHeader(
                    hasApiKey = uiState.hasApiKey,
                    onApiKeyClick = { showApiKeyDialog = true }
                )

                // Fehlender API Key Banner
                if (!uiState.hasApiKey) {
                    ApiKeyBanner(onClick = { showApiKeyDialog = true })
                }

                // Chat-Nachrichten Liste
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }

                    // Aktuell streamende Antwort
                    if (uiState.isStreaming) {
                        item {
                            StreamingBubble(text = uiState.currentStreamingResponse)
                        }
                    }
                }

                // Quick-Prompt Chips
                QuickPromptChips(
                    chips = uiState.quickPrompts,
                    enabled = !uiState.isStreaming && uiState.hasApiKey,
                    onChipClick = { prompt ->
                        viewModel.sendMessage(prompt)
                    }
                )

                // Eingabefeld unten
                ChatInputField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    isStreaming = uiState.isStreaming,
                    hasApiKey = uiState.hasApiKey,
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }
                )
            }
        }
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            hasExistingKey = uiState.hasApiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = { key ->
                viewModel.saveApiKey(key)
                showApiKeyDialog = false
            },
            onDelete = {
                viewModel.clearApiKey()
                showApiKeyDialog = false
            }
        )
    }
}

@Composable
private fun CoachHeader(
    hasApiKey: Boolean,
    onApiKeyClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "AeroCoach AI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Dein intelligenter Gemini 1.5 Ausdauertrainer",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }

        IconButton(onClick = onApiKeyClick) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "API-Key",
                tint = if (hasApiKey) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun ApiKeyBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33FF9800)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini API-Key fehlt für KI-Trainer Antworten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            TextButton(onClick = onClick) {
                Text("Eingeben", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isFromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) Color(0xFF1976D2) else Color(0xFF242C3D),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "Du" else "AeroCoach",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) Color(0xFFBBDEFB) else Color(0xFF64B5F6),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun StreamingBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = Color(0xFF242C3D),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AeroCoach denkt nach...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF64B5F6)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text.ifEmpty { "..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun QuickPromptChips(
    chips: List<String>,
    enabled: Boolean,
    onChipClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { prompt ->
            FilterChip(
                selected = false,
                onClick = { if (enabled) onChipClick(prompt) },
                enabled = enabled,
                label = { Text(prompt, style = MaterialTheme.typography.bodySmall) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF1E2638),
                    labelColor = Color.LightGray,
                    disabledContainerColor = Color(0xFF151A26),
                    disabledLabelColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isStreaming: Boolean,
    hasApiKey: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Frage an AeroCoach stellen...", color = Color.Gray) },
            maxLines = 4,
            enabled = !isStreaming,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1A2130),
                unfocusedContainerColor = Color(0xFF141A26),
                focusedBorderColor = Color(0xFF42A5F5),
                unfocusedBorderColor = Color(0xFF37474F),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = !isStreaming && value.isNotBlank() && hasApiKey,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (!isStreaming && value.isNotBlank() && hasApiKey) Color(0xFF1976D2) else Color(0xFF263238)
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Senden",
                tint = if (!isStreaming && value.isNotBlank() && hasApiKey) Color.White else Color.Gray
            )
        }
    }
}

@Composable
private fun ApiKeyDialog(
    hasExistingKey: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Google Gemini API-Key", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Gib deinen persönlichen API-Key aus Google AI Studio (aistudio.google.com) ein. Er wird sicher verschlüsselt auf deinem Gerät gespeichert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyInput) },
                enabled = keyInput.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            Row {
                if (hasExistingKey) {
                    TextButton(onClick = onDelete) {
                        Text("Key löschen", color = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )
}
