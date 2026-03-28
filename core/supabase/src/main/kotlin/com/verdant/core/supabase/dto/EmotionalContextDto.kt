package com.verdant.core.supabase.dto

import com.verdant.core.model.EmotionalContext
import com.verdant.core.model.InferredMood
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmotionalContextDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val date: Long,
    @SerialName("inferred_mood") val inferredMood: String,
    @SerialName("energy_level") val energyLevel: Int,
    val confidence: Float,
    @SerialName("contributing_signals") val contributingSignals: String,
    @SerialName("user_confirmed") val userConfirmed: Boolean,
)

fun EmotionalContextDto.toDomain(): EmotionalContext = EmotionalContext(
    id = id,
    date = date,
    inferredMood = InferredMood.valueOf(inferredMood),
    energyLevel = energyLevel,
    confidence = confidence,
    contributingSignals = contributingSignals,
    userConfirmed = userConfirmed,
)

fun EmotionalContext.toDto(userId: String): EmotionalContextDto = EmotionalContextDto(
    id = id,
    userId = userId,
    date = date,
    inferredMood = inferredMood.name,
    energyLevel = energyLevel,
    confidence = confidence,
    contributingSignals = contributingSignals,
    userConfirmed = userConfirmed,
)
