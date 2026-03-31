package com.verdant.core.model

data class StoryEvent(
    val id: String,
    val storyId: String,
    val eventType: StoryEventType,
    val referenceId: String? = null,
    val title: String,
    val description: String? = null,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val metadata: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long,
)

enum class StoryEventType {
    HABIT_COMPLETION,
    TRANSACTION,
    HEALTH_METRIC,
    ACTIVITY,
    LOCATION_VISIT,
    MOOD_CHECK,
    NOTE,
}
