package com.verdant.feature.habits.create

import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType

/**
 * Mutable working copy of a habit being created.
 * Converted to a [com.verdant.core.model.Habit] on save.
 */
data class HabitDraft(
    val name: String = "",
    val description: String = "",
    val icon: String = "🌱",
    val color: Long = 0xFF30A14EL,
    val label: String = "",
    val trackingType: TrackingType = TrackingType.BINARY,
    val unit: String = "",
    val targetValue: Double? = null,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    /** Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64 */
    val scheduleDays: Int = 0x7F,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "08:00",
    val reminderDays: Int = 0x7F,
    val streakGoal: Int? = null,
)

fun HabitTemplate.toDraft() = HabitDraft(
    name = name,
    description = "",
    icon = icon,
    color = color,
    label = label,
    trackingType = trackingType,
    unit = unit.orEmpty(),
    targetValue = targetValue,
    frequency = frequency,
    scheduleDays = scheduleDays,
    reminderEnabled = suggestedReminderTime != null,
    reminderTime = suggestedReminderTime ?: "08:00",
    reminderDays = scheduleDays,
)

fun ParsedHabit.toDraft() = HabitDraft(
    name = name,
    description = description.orEmpty(),
    icon = icon,
    color = color,
    label = label,
    trackingType = trackingType,
    unit = unit.orEmpty(),
    targetValue = targetValue,
    frequency = frequency,
    scheduleDays = scheduleDays,
    reminderEnabled = suggestedReminderTime != null,
    reminderTime = suggestedReminderTime ?: "08:00",
    reminderDays = scheduleDays,
)

val COLOR_PRESETS: List<Long> = listOf(
    0xFF30A14EL, // Verdant Green
    0xFF2196F3L, // Blue
    0xFF9C27B0L, // Purple
    0xFFE91E63L, // Pink
    0xFFFF5722L, // Deep Orange
    0xFFFF9800L, // Orange
    0xFFFFEB3BL, // Yellow
    0xFF009688L, // Teal
    0xFF00BCD4L, // Cyan
    0xFF3F51B5L, // Indigo
    0xFF795548L, // Brown
    0xFF607D8BL, // Blue Grey
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
