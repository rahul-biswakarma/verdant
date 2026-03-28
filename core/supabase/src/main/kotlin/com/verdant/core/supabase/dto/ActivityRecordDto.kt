package com.verdant.core.supabase.dto

import com.verdant.core.model.ActivityRecord
import com.verdant.core.model.ActivityType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityRecordDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("activity_type") val activityType: String,
    val confidence: Int,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("recorded_at") val recordedAt: Long,
    @SerialName("created_at") val createdAt: Long,
)

fun ActivityRecordDto.toDomain(): ActivityRecord = ActivityRecord(
    id = id,
    activityType = ActivityType.valueOf(activityType),
    confidence = confidence,
    durationMinutes = durationMinutes,
    recordedAt = recordedAt,
    createdAt = createdAt,
)

fun ActivityRecord.toDto(userId: String): ActivityRecordDto = ActivityRecordDto(
    id = id,
    userId = userId,
    activityType = activityType.name,
    confidence = confidence,
    durationMinutes = durationMinutes,
    recordedAt = recordedAt,
    createdAt = createdAt,
)
