package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.verdant.core.model.InsightType

@Entity(tableName = "ai_insights")
data class AIInsightEntity(
    @PrimaryKey val id: String,
    val type: InsightType,
    val content: String,
    @ColumnInfo(name = "related_habit_ids") val relatedHabitIds: List<String>,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    val dismissed: Boolean,
)
