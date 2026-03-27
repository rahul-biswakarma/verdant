package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_snapshots")
data class WeatherSnapshotEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val temperature: Double,
    val condition: String,
    val humidity: Int,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
