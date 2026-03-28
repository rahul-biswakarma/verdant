package com.verdant.core.supabase.dto

import com.verdant.core.model.Habit
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HabitDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    val description: String,
    val icon: String,
    val color: Long,
    val label: String? = null,
    @SerialName("tracking_type") val trackingType: String,
    @SerialName("visualization_type") val visualizationType: String,
    val unit: String? = null,
    @SerialName("target_value") val targetValue: Double? = null,
    @SerialName("checkpoint_steps") val checkpointSteps: String = "",
    val frequency: String,
    @SerialName("schedule_days") val scheduleDays: Int,
    @SerialName("is_archived") val isArchived: Boolean,
    @SerialName("reminder_enabled") val reminderEnabled: Boolean,
    @SerialName("reminder_time") val reminderTime: String? = null,
    @SerialName("reminder_days") val reminderDays: Int,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("outdoor_activity") val outdoorActivity: Boolean = false,
)

fun HabitDto.toDomain(): Habit = Habit(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    label = label,
    trackingType = TrackingType.valueOf(trackingType),
    visualizationType = VisualizationType.valueOf(visualizationType),
    unit = unit,
    targetValue = targetValue,
    checkpointSteps = if (checkpointSteps.isBlank()) emptyList() else checkpointSteps.split("|"),
    frequency = HabitFrequency.valueOf(frequency),
    scheduleDays = scheduleDays,
    isArchived = isArchived,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime,
    reminderDays = reminderDays,
    sortOrder = sortOrder,
    createdAt = createdAt,
    outdoorActivity = outdoorActivity,
)

fun Habit.toDto(userId: String): HabitDto = HabitDto(
    id = id,
    userId = userId,
    name = name,
    description = description,
    icon = icon,
    color = color,
    label = label,
    trackingType = trackingType.name,
    visualizationType = visualizationType.name,
    unit = unit,
    targetValue = targetValue,
    checkpointSteps = checkpointSteps.joinToString("|"),
    frequency = frequency.name,
    scheduleDays = scheduleDays,
    isArchived = isArchived,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime,
    reminderDays = reminderDays,
    sortOrder = sortOrder,
    createdAt = createdAt,
    outdoorActivity = outdoorActivity,
)
