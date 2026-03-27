package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "prediction_type") val predictionType: String,
    @ColumnInfo(name = "target_period") val targetPeriod: String,
    @ColumnInfo(name = "prediction_data") val predictionData: String,
    val confidence: Float,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
)
