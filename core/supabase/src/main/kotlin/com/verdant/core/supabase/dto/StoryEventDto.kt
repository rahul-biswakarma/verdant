package com.verdant.core.supabase.dto

import com.verdant.core.model.StoryEvent
import com.verdant.core.model.StoryEventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoryEventDto(
    val id: String,
    @SerialName("story_id") val storyId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("event_type") val eventType: String,
    @SerialName("reference_id") val referenceId: String? = null,
    val title: String,
    val description: String? = null,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val metadata: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long,
)

fun StoryEventDto.toDomain(): StoryEvent = StoryEvent(
    id = id,
    storyId = storyId,
    eventType = StoryEventType.valueOf(eventType),
    referenceId = referenceId,
    title = title,
    description = description,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude,
    metadata = metadata,
    sortOrder = sortOrder,
    createdAt = createdAt,
)

fun StoryEvent.toDto(userId: String): StoryEventDto = StoryEventDto(
    id = id,
    storyId = storyId,
    userId = userId,
    eventType = eventType.name,
    referenceId = referenceId,
    title = title,
    description = description,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude,
    metadata = metadata,
    sortOrder = sortOrder,
    createdAt = createdAt,
)
