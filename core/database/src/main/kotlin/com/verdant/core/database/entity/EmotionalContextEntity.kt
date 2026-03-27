package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emotional_context")
data class EmotionalContextEntity(
    @PrimaryKey val id: String,
    val date: Long,
    @ColumnInfo(name = "inferred_mood") val inferredMood: String,
    @ColumnInfo(name = "energy_level") val energyLevel: Int,
    val confidence: Float,
    @ColumnInfo(name = "contributing_signals") val contributingSignals: String,
    @ColumnInfo(name = "user_confirmed") val userConfirmed: Boolean,
)
