package com.verdant.core.ai

import com.verdant.core.model.EmotionalState
import com.verdant.core.model.InferredMood

data class LifeForecastContext(
    val currentEmotionalState: EmotionalState,
    val currentMood: InferredMood,
    val lifeScores: Map<String, Int>,
    val recentPatterns: List<String>,
    val upcomingEvents: List<String>,
    val habitCompletionRate7d: Float,
    val topRiskHabits: List<String>,
)

data class LifeForecast(
    val narrative: String,
    val keyPredictions: List<String>,
    val suggestedFocus: String,
    val confidence: Float,
)

data class HealthSummaryData(
    val stepsAvg7d: Double,
    val sleepAvg7d: Double,
    val heartRateAvg7d: Double,
    val exerciseMinutes7d: Double,
    val weightTrend: String,
)

data class HabitHistoryData(
    val habitName: String,
    val streakLength: Int,
    val completionRate30d: Float,
    val completionRate7d: Float,
    val dayOfWeekPattern: Map<Int, Float>,
    val missedReasons: List<String>,
)

data class SustainabilityPrediction(
    val score: Float,
    val riskFactors: List<String>,
    val suggestions: List<String>,
    val predictedDaysUntilBreak: Int?,
)

data class CrossDomainData(
    val habitCompletions: Map<String, List<Boolean>>,
    val sleepData: List<Double>,
    val screenTimeData: List<Double>,
    val spendingData: List<Double>,
    val exerciseData: List<Double>,
    val moodData: List<String>,
)
