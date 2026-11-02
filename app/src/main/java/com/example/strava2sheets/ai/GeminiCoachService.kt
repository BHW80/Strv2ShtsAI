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
    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            systemInstruction = content {
                text(systemPrompt)
            }
        )
    }

    /**
     * Sendet eine Nachricht an Gemini mit Chat-Historie und liefert die Antwort
     * als kontinuierlichen Streaming-Flow (Token/Text-Chunks).
     */
    fun sendMessageStream(
        history: List<ChatMessage>,
        userMessage: String
    ): Flow<String> = flow {
        // Konvertiere die UI-Historie in das SDK-Content-Format
        val sdkHistory: List<Content> = history.map { msg ->
            content(role = if (msg.isFromUser) "user" else "model") {
                text(msg.text)
            }
        }

        val chat = generativeModel.startChat(history = sdkHistory)
        val responseStream = chat.sendMessageStream(userMessage)

        responseStream.collect { chunk ->
            val chunkText = chunk.text
            if (!chunkText.isNullOrEmpty()) {
                emit(chunkText)
            }
        }
    }
}
