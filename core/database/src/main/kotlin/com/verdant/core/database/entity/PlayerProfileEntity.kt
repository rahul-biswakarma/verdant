package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: String,
    val level: Int,
    val title: String,
    @ColumnInfo(name = "total_xp") val totalXP: Long,
    @ColumnInfo(name = "current_level_xp") val currentLevelXP: Long,
    @ColumnInfo(name = "xp_to_next_level") val xpToNextLevel: Long,
    val rank: String,
    val stats: String,
    @ColumnInfo(name = "evolution_path") val evolutionPath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
