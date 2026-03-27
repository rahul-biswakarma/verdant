package com.verdant.core.prediction

import kotlin.math.exp

class HabitSustainabilityScorer {

    data class HabitHistory(
        val streakLength: Int,
        val completionRate: Float,
        val completionDecay: Float,
        val variance: Float,
    )

    fun score(history: HabitHistory): Float {
        val streakFactor = 1.0 / (1.0 + exp(-(history.streakLength - 14.0) / 7.0))
        val completionFactor = history.completionRate.toDouble()
        val decayPenalty = 1.0 - history.completionDecay.coerceIn(0f, 1f).toDouble()
        val variancePenalty = 1.0 - (history.variance.coerceIn(0f, 1f).toDouble() * 0.5)

        val raw = (streakFactor * 0.3 + completionFactor * 0.3 + decayPenalty * 0.25 + variancePenalty * 0.15)
        return raw.coerceIn(0.0, 1.0).toFloat()
    }
}
