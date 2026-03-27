package com.verdant.core.model

data class ActivityRecord(
    val id: String,
    val activityType: ActivityType,
    val confidence: Int,
    val durationMinutes: Int,
    val recordedAt: Long,
    val createdAt: Long,
)

enum class ActivityType {
    WALKING,
    RUNNING,
    CYCLING,
    STILL,
    IN_VEHICLE,
}
