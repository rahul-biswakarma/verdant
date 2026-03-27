package com.verdant.core.model

data class Prediction(
    val id: String,
    val predictionType: PredictionType,
    val targetPeriod: String,
    val predictionData: String,
    val confidence: Float,
    val generatedAt: Long,
    val expiresAt: Long,
)

enum class PredictionType {
    SPENDING_FORECAST,
    HABIT_SUSTAINABILITY,
    HEALTH_TRAJECTORY,
    LIFE_FORECAST,
}
