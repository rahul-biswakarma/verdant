package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "xp_reward") val xpReward: Int,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long,
    val category: String,
)
