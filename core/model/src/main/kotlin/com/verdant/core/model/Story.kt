package com.verdant.core.model

data class Story(
    val id: String,
    val title: String,
    val description: String? = null,
    val template: StoryTemplate? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val coverEmoji: String = "\uD83D\uDCD6",
    val aiSummary: String? = null,
    val aiInsights: String? = null,
    val isAutoDetected: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class StoryTemplate {
    ROAD_TRIP,
    WORKOUT_SESSION,
    SHOPPING_TRIP,
    COMMUTE,
    WORK_DAY,
    EVENING_ROUTINE,
    CUSTOM,
}
