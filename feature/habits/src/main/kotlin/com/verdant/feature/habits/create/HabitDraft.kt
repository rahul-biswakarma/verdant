package com.verdant.feature.habits.create

import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType
import com.verdant.core.model.defaultVisualization

/**
 * Mutable working copy of a habit being created.
 * Converted to a [com.verdant.core.model.Habit] on save.
 */
data class HabitDraft(
    val name: String = "",
    val description: String = "",
    val icon: String = "🌱",
    val color: Long = 0xFF5A7A60L,
    val label: String = "",
    val trackingType: TrackingType = TrackingType.BINARY,
    val visualizationType: VisualizationType = VisualizationType.PIXEL_GRID,
    val unit: String = "",
    val targetValue: Double? = null,
    /** Ordered milestone steps; only used when trackingType == CHECKPOINT. */
    val checkpointSteps: List<String> = listOf("", "", ""),
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    /** Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64 */
    val scheduleDays: Int = 0x7F,
    val reminderEnabled: Boolean = false,
    /** Comma-separated reminder times (e.g., "07:00,19:00" for morning and evening). */
    val reminderTimes: List<String> = listOf("08:00"),
    val reminderDays: Int = 0x7F,
    val streakGoal: Int? = null,
) {
    /** Joined reminder times for storage in the Habit model. */
    val reminderTimeJoined: String get() = reminderTimes.joinToString(",")
}

fun HabitTemplate.toDraft() = HabitDraft(
    name = name,
    description = "",
    icon = icon,
    color = color,
    label = label,
    trackingType = trackingType,
    visualizationType = visualizationType,
    unit = unit.orEmpty(),
    targetValue = targetValue,
    checkpointSteps = checkpointSteps.ifEmpty { listOf("", "", "") },
    frequency = frequency,
    scheduleDays = scheduleDays,
    reminderEnabled = suggestedReminderTime != null,
    reminderTimes = suggestedReminderTime?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        ?: listOf("08:00"),
    reminderDays = scheduleDays,
)

fun ParsedHabit.toDraft() = HabitDraft(
    name = name,
    description = description.orEmpty(),
    icon = icon,
    color = color,
    label = label,
    trackingType = trackingType,
    visualizationType = trackingType.defaultVisualization(),
    unit = unit.orEmpty(),
    targetValue = targetValue,
    checkpointSteps = listOf("", "", ""),
    frequency = frequency,
    scheduleDays = scheduleDays,
    reminderEnabled = suggestedReminderTimes.isNotEmpty(),
    reminderTimes = suggestedReminderTimes.ifEmpty { listOf("08:00") },
    reminderDays = scheduleDays,
)

val COLOR_PRESETS: List<Long> = listOf(
    0xFF5A7A60L, // Muted Sage
    0xFF6B8E8AL, // Warm Teal
    0xFF8B7355L, // Warm Bronze
    0xFF7B6B6BL, // Dusty Mauve
    0xFFE8673CL, // Burnt Orange
    0xFF9B6B6BL, // Dusty Rose
    0xFF5A6B7AL, // Slate
    0xFF8A7B5AL, // Olive
    0xFF6B5B8AL, // Muted Purple
    0xFF3F51B5L, // Indigo
    0xFF2196F3L, // Blue
    0xFF795548L, // Brown
)

val ICON_PRESETS: List<String> = listOf(
    "🌱", "💪", "🧠", "❤️", "🏃", "🚴", "🧘", "📚",
    "💻", "🍎", "💧", "😴", "💰", "📝", "🎯", "🏋️",
    "🎸", "🌿", "☀️", "🔥", "⭐", "🦋", "🌊", "🏔️",
)

/** Day labels and their bitmask values in Mon-first order */
val SCHEDULE_DAYS: List<Pair<String, Int>> = listOf(
    "M" to 1, "T" to 2, "W" to 4, "T" to 8, "F" to 16, "S" to 32, "S" to 64,
)
