package com.verdant.core.model

data class PendingAIRequest(
    val id: String,
    val requestType: String,
    val payload: String,
    val createdAt: Long,
    val attemptCount: Int,
)
