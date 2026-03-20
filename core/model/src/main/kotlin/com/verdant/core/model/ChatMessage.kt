package com.verdant.core.model

data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val habitContext: String?,
    val timestamp: Long,
)
