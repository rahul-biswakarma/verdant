package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_cache")
data class StreakCacheEntity(
    @PrimaryKey @ColumnInfo(name = "habit_id") val habitId: String,
    @ColumnInfo(name = "current_streak") val currentStreak: Int,
    @ColumnInfo(name = "longest_streak") val longestStreak: Int,
    @ColumnInfo(name = "completion_rate") val completionRate: Float,
    @ColumnInfo(name = "cached_at") val cachedAt: Long,
)
