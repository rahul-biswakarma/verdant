package com.verdant.core.emotional

import com.verdant.core.model.EmotionalContext
import com.verdant.core.model.InferredMood

class SignalFusionLayer {

    data class MicroSignals(
        val habitCompletionRate: Float,
        val screenTimeMinutes: Double,
        val sleepHours: Double,
        val exerciseMinutes: Double,
        val spendingRatio: Double,
        val notificationCount: Int,
        val calendarBusyHours: Double,
    )

    fun fuse(signals: MicroSignals): FusionResult {
        val mood = inferMood(signals)
        val energy = inferEnergy(signals)
        val confidence = calculateConfidence(signals)

        return FusionResult(mood = mood, energyLevel = energy, confidence = confidence)
    }

    private fun inferMood(signals: MicroSignals): InferredMood {
        val positiveSignals = listOf(
            signals.habitCompletionRate > 0.7f,
            signals.sleepHours >= 7.0,
            signals.exerciseMinutes >= 20.0,
            signals.screenTimeMinutes < 180.0,
        ).count { it }

        val negativeSignals = listOf(
            signals.habitCompletionRate < 0.3f,
            signals.sleepHours < 5.0,
            signals.spendingRatio > 1.5,
            signals.notificationCount > 100,
            signals.calendarBusyHours > 8.0,
        ).count { it }

        return when {
            positiveSignals >= 3 && negativeSignals == 0 -> InferredMood.ENERGIZED
            negativeSignals >= 3 -> InferredMood.STRESSED
            negativeSignals >= 2 && signals.sleepHours < 5.0 -> InferredMood.ANXIOUS
            negativeSignals >= 2 -> InferredMood.LOW
            else -> InferredMood.NEUTRAL
        }
    }

    private fun inferEnergy(signals: MicroSignals): Int {
        val sleepScore = (signals.sleepHours / 8.0 * 40).coerceAtMost(40.0)
        val exerciseScore = (signals.exerciseMinutes / 30.0 * 20).coerceAtMost(20.0)
        val completionScore = signals.habitCompletionRate * 20
        val screenPenalty = ((signals.screenTimeMinutes - 120) / 60.0 * 10).coerceIn(0.0, 20.0)

        return (sleepScore + exerciseScore + completionScore - screenPenalty)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun calculateConfidence(signals: MicroSignals): Float {
        var dataPoints = 0
        if (signals.sleepHours > 0) dataPoints++
        if (signals.exerciseMinutes > 0) dataPoints++
        if (signals.screenTimeMinutes > 0) dataPoints++
        if (signals.notificationCount > 0) dataPoints++
        if (signals.calendarBusyHours > 0) dataPoints++

        return (dataPoints.toFloat() / 5f).coerceIn(0.2f, 1.0f)
    }

    data class FusionResult(
        val mood: InferredMood,
        val energyLevel: Int,
        val confidence: Float,
    )
}
