package com.verdant.core.supabase.dto

import com.verdant.core.model.Story
import com.verdant.core.model.StoryTemplate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoryDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val description: String? = null,
    val template: String? = null,
    @SerialName("start_time") val startTime: Long,
    @SerialName("end_time") val endTime: Long? = null,
    @SerialName("cover_emoji") val coverEmoji: String = "\uD83D\uDCD6",
    @SerialName("ai_summary") val aiSummary: String? = null,
    @SerialName("ai_insights") val aiInsights: String? = null,
    @SerialName("is_auto_detected") val isAutoDetected: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

fun StoryDto.toDomain(): Story = Story(
    id = id,
    title = title,
    description = description,
    template = template?.let { runCatching { StoryTemplate.valueOf(it) }.getOrNull() },
    startTime = startTime,
    endTime = endTime,
    coverEmoji = coverEmoji,
    aiSummary = aiSummary,
    aiInsights = aiInsights,
    isAutoDetected = isAutoDetected,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Story.toDto(userId: String): StoryDto = StoryDto(
    id = id,
    userId = userId,
    title = title,
    description = description,
    template = template?.name,
    startTime = startTime,
    endTime = endTime,
    coverEmoji = coverEmoji,
    aiSummary = aiSummary,
    aiInsights = aiInsights,
    isAutoDetected = isAutoDetected,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
