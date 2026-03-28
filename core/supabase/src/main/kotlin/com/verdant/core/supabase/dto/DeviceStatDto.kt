package com.verdant.core.supabase.dto

import com.verdant.core.model.DeviceStat
import com.verdant.core.model.DeviceStatType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceStatDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("stat_type") val statType: String,
    val value: Double,
    val detail: String? = null,
    @SerialName("recorded_date") val recordedDate: Long,
    @SerialName("created_at") val createdAt: Long,
)

fun DeviceStatDto.toDomain(): DeviceStat = DeviceStat(
    id = id,
    statType = DeviceStatType.valueOf(statType),
    value = value,
    detail = detail,
    recordedDate = recordedDate,
    createdAt = createdAt,
)

fun DeviceStat.toDto(userId: String): DeviceStatDto = DeviceStatDto(
    id = id,
    userId = userId,
    statType = statType.name,
    value = value,
    detail = detail,
    recordedDate = recordedDate,
    createdAt = createdAt,
)
