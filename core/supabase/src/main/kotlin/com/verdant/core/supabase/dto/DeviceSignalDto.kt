package com.verdant.core.supabase.dto

import com.verdant.core.model.DeviceSignal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceSignalDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("device_id") val deviceId: String,
    @SerialName("signal_type") val signalType: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
    @SerialName("created_at") val createdAt: Long,
)

fun DeviceSignalDto.toDomain(): DeviceSignal = DeviceSignal(
    id = id,
    deviceId = deviceId,
    signalType = signalType,
    value = value,
    unit = unit,
    timestamp = timestamp,
    createdAt = createdAt,
)

fun DeviceSignal.toDto(userId: String): DeviceSignalDto = DeviceSignalDto(
    id = id,
    userId = userId,
    deviceId = deviceId,
    signalType = signalType,
    value = value,
    unit = unit,
    timestamp = timestamp,
    createdAt = createdAt,
)
