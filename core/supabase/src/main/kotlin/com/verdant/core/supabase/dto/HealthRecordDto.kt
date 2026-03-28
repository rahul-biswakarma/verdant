package com.verdant.core.supabase.dto

import com.verdant.core.model.HealthRecord
import com.verdant.core.model.HealthRecordType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthRecordDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("record_type") val recordType: String,
    val value: Double,
    @SerialName("secondary_value") val secondaryValue: Double? = null,
    val unit: String,
    @SerialName("recorded_at") val recordedAt: Long,
    val source: String,
    @SerialName("created_at") val createdAt: Long,
)

fun HealthRecordDto.toDomain(): HealthRecord = HealthRecord(
    id = id,
    recordType = HealthRecordType.valueOf(recordType),
    value = value,
    secondaryValue = secondaryValue,
    unit = unit,
    recordedAt = recordedAt,
    source = source,
    createdAt = createdAt,
)

fun HealthRecord.toDto(userId: String): HealthRecordDto = HealthRecordDto(
    id = id,
    userId = userId,
    recordType = recordType.name,
    value = value,
    secondaryValue = secondaryValue,
    unit = unit,
    recordedAt = recordedAt,
    source = source,
    createdAt = createdAt,
)
