package com.verdant.core.supabase.dto

import com.verdant.core.model.Prediction
import com.verdant.core.model.PredictionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PredictionDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("prediction_type") val predictionType: String,
    @SerialName("target_period") val targetPeriod: String,
    @SerialName("prediction_data") val predictionData: String,
    val confidence: Float,
    @SerialName("generated_at") val generatedAt: Long,
    @SerialName("expires_at") val expiresAt: Long,
)

fun PredictionDto.toDomain(): Prediction = Prediction(
    id = id,
    predictionType = PredictionType.valueOf(predictionType),
    targetPeriod = targetPeriod,
    predictionData = predictionData,
    confidence = confidence,
    generatedAt = generatedAt,
    expiresAt = expiresAt,
)

fun Prediction.toDto(userId: String): PredictionDto = PredictionDto(
    id = id,
    userId = userId,
    predictionType = predictionType.name,
    targetPeriod = targetPeriod,
    predictionData = predictionData,
    confidence = confidence,
    generatedAt = generatedAt,
    expiresAt = expiresAt,
)
