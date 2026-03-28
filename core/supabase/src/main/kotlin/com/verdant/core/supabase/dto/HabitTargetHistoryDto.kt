package com.verdant.core.supabase.dto

import com.verdant.core.model.HabitTargetHistory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HabitTargetHistoryDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("habit_id") val habitId: String,
    @SerialName("old_target") val oldTarget: Double,
    @SerialName("new_target") val newTarget: Double,
    @SerialName("changed_at") val changedAt: Long,
    val reason: String,
)

fun HabitTargetHistoryDto.toDomain(): HabitTargetHistory = HabitTargetHistory(
    id = id,
    habitId = habitId,
    oldTarget = oldTarget,
    newTarget = newTarget,
    changedAt = changedAt,
    reason = reason,
)

fun HabitTargetHistory.toDto(userId: String): HabitTargetHistoryDto = HabitTargetHistoryDto(
    id = id,
    userId = userId,
    habitId = habitId,
    oldTarget = oldTarget,
    newTarget = newTarget,
    changedAt = changedAt,
    reason = reason,
)
