package com.verdant.core.prediction

import kotlin.math.sqrt

class StressIndexCalculator {

    data class StressSignals(
        val screenTimeMinutes: Double,
        val notificationCount: Int,
        val sleepHours: Double,
        val spendingRatio: Double,
        val habitMissCount: Int,
    )

    data class BaselineStats(
        val avgScreenTime: Double,
        val stdScreenTime: Double,
        val avgNotifications: Double,
        val stdNotifications: Double,
        val avgSleep: Double,
        val stdSleep: Double,
        val avgSpendingRatio: Double,
        val stdSpendingRatio: Double,
        val avgMisses: Double,
        val stdMisses: Double,
    )

    fun calculate(signals: StressSignals, baseline: BaselineStats): Float {
        val screenTimeZ = zScore(signals.screenTimeMinutes, baseline.avgScreenTime, baseline.stdScreenTime)
        val notificationZ = zScore(signals.notificationCount.toDouble(), baseline.avgNotifications, baseline.stdNotifications)
        val sleepZ = -zScore(signals.sleepHours, baseline.avgSleep, baseline.stdSleep) // inverted: less sleep = more stress
        val spendingZ = zScore(signals.spendingRatio, baseline.avgSpendingRatio, baseline.stdSpendingRatio)
        val missZ = zScore(signals.habitMissCount.toDouble(), baseline.avgMisses, baseline.stdMisses)

        val composite = (screenTimeZ * 0.2 + notificationZ * 0.15 + sleepZ * 0.3 + spendingZ * 0.15 + missZ * 0.2)
        val normalized = (composite + 3.0) / 6.0
        return normalized.coerceIn(0.0, 1.0).toFloat()
    }

    private fun zScore(value: Double, mean: Double, std: Double): Double {
        if (std == 0.0) return 0.0
        return (value - mean) / std
    }
}
