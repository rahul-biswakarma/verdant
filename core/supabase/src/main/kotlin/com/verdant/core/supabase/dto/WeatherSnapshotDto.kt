package com.verdant.core.supabase.dto

import com.verdant.core.model.WeatherCondition
import com.verdant.core.model.WeatherSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherSnapshotDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val date: Long,
    val temperature: Double,
    val condition: String,
    val humidity: Int,
    val latitude: Double,
    val longitude: Double,
    @SerialName("created_at") val createdAt: Long,
)

fun WeatherSnapshotDto.toDomain(): WeatherSnapshot = WeatherSnapshot(
    id = id,
    date = date,
    temperature = temperature,
    condition = WeatherCondition.valueOf(condition),
    humidity = humidity,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
)

fun WeatherSnapshot.toDto(userId: String): WeatherSnapshotDto = WeatherSnapshotDto(
    id = id,
    userId = userId,
    date = date,
    temperature = temperature,
    condition = condition.name,
    humidity = humidity,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
)
