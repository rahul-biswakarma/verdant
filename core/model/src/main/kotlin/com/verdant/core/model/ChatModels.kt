package com.verdant.core.model

/**
 * A single message in a coach conversation.
 *
 * [role] is one of "user" or "assistant" — mirrors [ChatMessage.role].
 * [isStreaming] is true while the assistant is still generating its reply
 * (shows a typing indicator on the last message).
 */
data class ChatBubble(
    val id: String,
    val role: String,       // "user" | "assistant"
    val content: String,
    val timestamp: Long,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
)

data class ChatState(
    val messages: List<ChatBubble> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    /** Shown below the input bar when a network/rate error occurs. */
    val sendError: String? = null,
)
