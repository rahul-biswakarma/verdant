package com.verdant.core.supabase.dto

import com.verdant.core.model.PendingAIRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PendingAIRequestDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("request_type") val requestType: String,
    val payload: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("attempt_count") val attemptCount: Int,
)

fun PendingAIRequestDto.toDomain(): PendingAIRequest = PendingAIRequest(
    id = id,
    requestType = requestType,
    payload = payload,
    createdAt = createdAt,
    attemptCount = attemptCount,
)

fun PendingAIRequest.toDto(userId: String): PendingAIRequestDto = PendingAIRequestDto(
    id = id,
    userId = userId,
    requestType = requestType,
    payload = payload,
    createdAt = createdAt,
    attemptCount = attemptCount,
)
