package com.example.strava2sheets.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class GeminiCoachService(
    private val apiKey: String,
    private val systemPrompt: String
) {
    // Liste von Fallback-Modellen (aktuelle Google AI Studio Modelle)
    private val modelCandidates = listOf(
        "gemini-2.0-flash",
        "gemini-2.0-flash-exp",
        "gemini-2.5-flash",
        "gemini-1.5-flash-latest",
        "gemini-1.5-flash-8b",
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-pro"
    )

    private fun createModel(modelName: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            systemInstruction = content {
                text(systemPrompt)
            }
        )
    }

    /**
     * Sendet eine Nachricht an Gemini mit Chat-Historie und liefert die Antwort
     * als kontinuierlichen Streaming-Flow (Token/Text-Chunks).
     * Falls ein Modell-Name deprecated / nicht verfügbar ist, wird automatisch das nächste versucht.
     */
    fun sendMessageStream(
        history: List<ChatMessage>,
        userMessage: String
    ): Flow<String> = flow {
        val sdkHistory: List<Content> = history.map { msg ->
            content(role = if (msg.isFromUser) "user" else "model") {
                text(msg.text)
            }
        }

        var lastException: Exception? = null
        var success = false

        for (candidate in modelCandidates) {
            try {
                val model = createModel(candidate)
                val chat = model.startChat(history = sdkHistory)
                val responseStream = chat.sendMessageStream(userMessage)

                responseStream.collect { chunk ->
                    val chunkText = chunk.text
                    if (!chunkText.isNullOrEmpty()) {
                        success = true
                        emit(chunkText)
                    }
                }
                if (success) {
                    return@flow
                }
            } catch (e: Exception) {
                lastException = e
                // Falls Fehler (z.B. 404 Not Found, 400 Unsupported, etc.) -> nächstes Modell probieren
                continue
            }
        }

        if (!success && lastException != null) {
            throw lastException
        }
    }
}
