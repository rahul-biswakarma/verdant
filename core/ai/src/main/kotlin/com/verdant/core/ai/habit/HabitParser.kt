package com.verdant.core.ai.habit

import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType

interface HabitParser {
    suspend fun parseHabitDescription(description: String): ParsedHabit
}

data class ParsedHabit(
    val name: String,
    val icon: String,
    val color: Long,
    val label: String,
    val trackingType: TrackingType,
    val unit: String?,
    val targetValue: Double?,
    val frequency: HabitFrequency,
    val scheduleDays: Int,
    val suggestedReminderTime: String?,
    val description: String?,
) {
    /** All suggested reminder times (supports "morning and evening" style input). */
    val suggestedReminderTimes: List<String>
        get() = suggestedReminderTime
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
}
