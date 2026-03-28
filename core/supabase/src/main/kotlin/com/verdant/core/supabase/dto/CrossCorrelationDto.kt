package com.verdant.core.supabase.dto

import com.verdant.core.model.CrossCorrelation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CrossCorrelationDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("dimension_a") val dimensionA: String,
    @SerialName("dimension_b") val dimensionB: String,
    @SerialName("correlation_strength") val correlationStrength: Float,
    val description: String,
    @SerialName("discovered_at") val discoveredAt: Long,
    @SerialName("sample_size") val sampleSize: Int,
)

fun CrossCorrelationDto.toDomain(): CrossCorrelation = CrossCorrelation(
    id = id,
    dimensionA = dimensionA,
    dimensionB = dimensionB,
    correlationStrength = correlationStrength,
    description = description,
    discoveredAt = discoveredAt,
    sampleSize = sampleSize,
)

fun CrossCorrelation.toDto(userId: String): CrossCorrelationDto = CrossCorrelationDto(
    id = id,
    userId = userId,
    dimensionA = dimensionA,
    dimensionB = dimensionB,
    correlationStrength = correlationStrength,
    description = description,
    discoveredAt = discoveredAt,
    sampleSize = sampleSize,
)
