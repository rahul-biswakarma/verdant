package com.verdant.core.supabase.dto

import com.verdant.core.model.AIInsight
import com.verdant.core.model.InsightType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AIInsightDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val type: String,
    val content: String,
    @SerialName("related_habit_ids") val relatedHabitIds: String,
    @SerialName("generated_at") val generatedAt: Long,
    @SerialName("expires_at") val expiresAt: Long,
    val dismissed: Boolean,
)

fun AIInsightDto.toDomain(): AIInsight = AIInsight(
    id = id,
    type = InsightType.valueOf(type),
    content = content,
    relatedHabitIds = if (relatedHabitIds.isBlank()) emptyList() else relatedHabitIds.split(","),
    generatedAt = generatedAt,
    expiresAt = expiresAt,
    dismissed = dismissed,
)

fun AIInsight.toDto(userId: String): AIInsightDto = AIInsightDto(
    id = id,
    userId = userId,
    type = type.name,
    content = content,
    relatedHabitIds = relatedHabitIds.joinToString(","),
    generatedAt = generatedAt,
    expiresAt = expiresAt,
    dismissed = dismissed,
)
