package com.example.strava2sheets.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.strava2sheets.ai.ChatMessage
import com.example.strava2sheets.ai.CoachBiometrics
import com.example.strava2sheets.ai.GeminiCoachService
import com.example.strava2sheets.ai.TrainingContextBuilder
import com.example.strava2sheets.data.local.ActivityDao
import com.example.strava2sheets.data.local.AthleteProfileDao
import com.example.strava2sheets.data.security.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoachUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val currentStreamingResponse: String = "",
    val hasApiKey: Boolean = false,
    val errorMessage: String? = null,
    val quickPrompts: List<String> = listOf(
        "Wie ist meine aktuelle Form?",
        "Kann ich heute intensiv trainieren?",
        "Analysiere meine letzten 2 Wochen",
        "Empfehlung für Ruhetag & Regeneration"
    )
)

class CoachChatViewModel(
    private val tokenStorage: TokenStorage,
    private val activityDao: ActivityDao,
    private val athleteProfileDao: AthleteProfileDao?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    init {
        checkApiKey()
        // Begrüßungsnachricht von AeroCoach
        val greeting = ChatMessage(
            text = "Hallo! Ich bin **AeroCoach**, dein persönlicher KI-Trainer. Ich habe Zugriff auf dein Profil, deine Formkurve (CTL/ATL/TSB) und deine Einheiten.\n\nWie kann ich dir bei deiner Trainingsplanung helfen?",
            isFromUser = false
        )
        _uiState.update { it.copy(messages = listOf(greeting)) }
    }

    fun checkApiKey() {
        val hasKey = tokenStorage.hasGeminiApiKey()
        _uiState.update { it.copy(hasApiKey = hasKey) }
    }

    fun saveApiKey(key: String) {
        if (key.isNotBlank()) {
            tokenStorage.saveGeminiApiKey(key.trim())
            checkApiKey()
        }
    }

    fun clearApiKey() {
        tokenStorage.clearGeminiApiKey()
        checkApiKey()
    }

    fun sendMessage(userText: String) {
        val text = userText.trim()
        if (text.isEmpty() || _uiState.value.isStreaming) return

        val apiKey = tokenStorage.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Bitte hinterlege zuerst deinen Gemini API-Key!") }
            return
        }

        val userMessage = ChatMessage(text = text, isFromUser = true)
        val updatedMessages = _uiState.value.messages + userMessage

        _uiState.update {
            it.copy(
                messages = updatedMessages,
                isStreaming = true,
                currentStreamingResponse = "",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // Lade Room-Daten
                val activities = activityDao.getAllActivitiesList()
                val athlete = athleteProfileDao?.getLatestProfile()

                // Erstelle kompakten System-Prompt (~2.000 Tokens)
                val systemPrompt = TrainingContextBuilder.buildSystemPrompt(
                    athlete = athlete,
                    activities = activities,
                    biometrics = CoachBiometrics()
                )

                val coachService = GeminiCoachService(
                    apiKey = apiKey,
                    systemPrompt = systemPrompt
                )

                val sb = java.lang.StringBuilder()
                coachService.sendMessageStream(
                    history = updatedMessages.dropLast(1), // bisheriger Verlauf ohne die neue User-Nachricht
                    userMessage = text
                ).collect { chunk ->
                    sb.append(chunk)
                    _uiState.update {
                        it.copy(currentStreamingResponse = sb.toString())
                    }
                }

                // Streaming abgeschlossen: Nachricht final in Historie ablegen
                val aiMessage = ChatMessage(
                    text = sb.toString(),
                    isFromUser = false
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + aiMessage,
                        isStreaming = false,
                        currentStreamingResponse = ""
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        currentStreamingResponse = "",
                        errorMessage = "Fehler bei der Kommunikation mit AeroCoach: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
