package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_records")
data class HealthRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "record_type") val recordType: String,
    val value: Double,
    @ColumnInfo(name = "secondary_value") val secondaryValue: Double?,
    val unit: String,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
    val source: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
