package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_signals")
data class DeviceSignalEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "signal_type") val signalType: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
