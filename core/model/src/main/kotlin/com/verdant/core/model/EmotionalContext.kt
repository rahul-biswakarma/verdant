package com.verdant.core.model

data class EmotionalContext(
    val id: String,
    val date: Long,
    val inferredMood: InferredMood,
    val energyLevel: Int,
    val confidence: Float,
    val contributingSignals: String,
    val userConfirmed: Boolean,
)

enum class InferredMood {
    ENERGIZED,
    NEUTRAL,
    LOW,
    STRESSED,
    ANXIOUS,
}
