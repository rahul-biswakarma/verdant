package com.verdant.core.supabase.dto

import com.verdant.core.model.StreakCache
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreakCacheDto(
    @SerialName("habit_id") val habitId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("current_streak") val currentStreak: Int,
    @SerialName("longest_streak") val longestStreak: Int,
    @SerialName("completion_rate") val completionRate: Float,
    @SerialName("cached_at") val cachedAt: Long,
)

fun StreakCacheDto.toDomain(): StreakCache = StreakCache(
    habitId = habitId,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    completionRate = completionRate,
    cachedAt = cachedAt,
)

fun StreakCache.toDto(userId: String): StreakCacheDto = StreakCacheDto(
    habitId = habitId,
    userId = userId,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    completionRate = completionRate,
    cachedAt = cachedAt,
)
