package com.verdant.core.ai

/**
 * Routes AI requests between on-device (Gemini Nano via ML Kit GenAI)
 * and cloud (Claude API) based on task complexity and connectivity.
 */
interface AiRouter {
    suspend fun generateInsight(prompt: String): String
    suspend fun chat(messages: List<ChatMessage>): String
}

data class ChatMessage(
    val role: Role,
    val content: String,
) {
    enum class Role { USER, ASSISTANT }
}
