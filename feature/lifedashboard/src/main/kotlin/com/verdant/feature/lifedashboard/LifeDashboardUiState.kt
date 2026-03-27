package com.verdant.feature.lifedashboard

import com.verdant.core.model.EmotionalState
import com.verdant.core.model.InferredMood
import com.verdant.core.model.PlayerProfile
import com.verdant.core.model.Quest

data class LifeDashboardUiState(
    val isLoading: Boolean = true,
    val playerProfile: PlayerProfile? = null,
    val emotionalState: EmotionalState = EmotionalState.FLOW,
    val currentMood: InferredMood = InferredMood.NEUTRAL,
    val energyLevel: Int = 50,
    val statDimensions: List<StatDimension> = emptyList(),
    val activeQuests: List<Quest> = emptyList(),
    val predictions: List<PredictionCard> = emptyList(),
    val lifeForecastNarrative: String? = null,
    val systemInsight: String? = null,
    val dangerZoneHabits: List<DangerZoneHabit> = emptyList(),
)

data class StatDimension(
    val name: String,
    val score: Int,
    val trend: Trend,
    val iconName: String,
)

enum class Trend { UP, DOWN, FLAT }

data class PredictionCard(
    val title: String,
    val summary: String,
    val confidence: Float,
)

data class DangerZoneHabit(
    val habitId: String,
    val habitName: String,
    val riskScore: Float,
)
