package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "life_scores")
data class LifeScoreEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "score_type") val scoreType: String,
    val score: Int,
    val components: String,
    @ColumnInfo(name = "computed_date") val computedDate: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
