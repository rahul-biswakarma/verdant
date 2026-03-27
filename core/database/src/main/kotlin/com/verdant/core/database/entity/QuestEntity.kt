package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    @ColumnInfo(name = "xp_reward") val xpReward: Int,
    val conditions: String,
    @ColumnInfo(name = "time_limit") val timeLimit: Long?,
    @ColumnInfo(name = "generated_by") val generatedBy: String,
    val reasoning: String,
    val status: String,
    @ColumnInfo(name = "started_at") val startedAt: Long?,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
)
