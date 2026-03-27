package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_stats")
data class DeviceStatEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "stat_type") val statType: String,
    val value: Double,
    val detail: String?,
    @ColumnInfo(name = "recorded_date") val recordedDate: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
