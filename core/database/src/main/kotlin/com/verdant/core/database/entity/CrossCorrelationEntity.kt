package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cross_correlations")
data class CrossCorrelationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "dimension_a") val dimensionA: String,
    @ColumnInfo(name = "dimension_b") val dimensionB: String,
    @ColumnInfo(name = "correlation_strength") val correlationStrength: Float,
    val description: String,
    @ColumnInfo(name = "discovered_at") val discoveredAt: Long,
    @ColumnInfo(name = "sample_size") val sampleSize: Int,
)
