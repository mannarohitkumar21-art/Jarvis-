package com.example.model

import java.util.UUID

enum class AiProviderType(val displayName: String, val isLocal: Boolean, val defaultModel: String) {
    GEMINI("Google Gemini", false, "gemini-3.5-flash"),
    OPENAI("OpenAI Compatible", false, "gpt-4o-mini"),
    GROQ("Groq Cloud", false, "llama-3.3-70b-versatile"),
    OPENROUTER("OpenRouter", false, "google/gemini-2.0-flash-exp:free"),
    OFFLINE_CORE("JARVIS Offline Engine", true, "rule-based-core")
}

data class AiConfig(
    val selectedProvider: AiProviderType = AiProviderType.GEMINI,
    val selectedModel: String = "gemini-3.5-flash",
    val customApiKey: String = "",
    val customEndpoint: String = "",
    val temperature: Float = 0.4f,
    val systemPrompt: String = "You are JARVIS AI, an autonomous personal Android assistant with terminal and device automation abilities. Always respond concisely and authoritatively."
)

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val associatedPlan: TaskPlan? = null,
    val executionCommand: String? = null
)
