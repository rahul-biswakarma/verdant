package com.verdant.core.emotional

class RiskScoringEngine {

    data class HabitRiskInput(
        val hoursIntoDayRatio: Float,
        val typicalCompletionWindowPassed: Boolean,
        val dayOfWeekFailureRate: Float,
        val currentStreak: Int,
        val recentMissesIn7Days: Int,
        val isCompletedToday: Boolean,
    )

    fun computeRisk(input: HabitRiskInput): Float {
        if (input.isCompletedToday) return 0f

        val timePressure = input.hoursIntoDayRatio.coerceIn(0f, 1f)
        val windowPassed = if (input.typicalCompletionWindowPassed) 0.2f else 0f
        val dowFailure = input.dayOfWeekFailureRate.coerceIn(0f, 1f)

        val streakUrgency = when {
            input.currentStreak >= 30 -> 0.3f
            input.currentStreak >= 14 -> 0.2f
            input.currentStreak >= 7 -> 0.15f
            input.currentStreak >= 3 -> 0.1f
            else -> 0f
        }

        val recentMissDensity = (input.recentMissesIn7Days / 7f).coerceIn(0f, 1f)

        val risk = (timePressure * 0.25f +
                windowPassed +
                dowFailure * 0.2f +
                streakUrgency +
                recentMissDensity * 0.15f)

        return risk.coerceIn(0f, 1f)
    }
}
