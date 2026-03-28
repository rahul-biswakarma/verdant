package com.verdant.core.model

data class StreakCache(
    val habitId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Float,
    val cachedAt: Long,
)
