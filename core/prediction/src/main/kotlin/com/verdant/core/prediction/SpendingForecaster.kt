package com.verdant.core.prediction

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class SpendingForecaster @Inject constructor() {

    data class MonthlySpending(val month: Long, val amount: Double)

    /**
     * Predicts spending for [monthsAhead] months into the future using linear regression
     * on the provided [history]. Returns 0.0 if insufficient data or prediction is negative.
     */
    fun forecast(history: List<MonthlySpending>, monthsAhead: Int = 1): Double {
        if (history.size < 2) return history.lastOrNull()?.amount ?: 0.0

        val n = history.size
        val xMean = (n - 1) / 2.0
        val yMean = history.map { it.amount }.average()

        var numerator = 0.0
        var denominator = 0.0
        history.forEachIndexed { i, data ->
            val xDiff = i - xMean
            numerator += xDiff * (data.amount - yMean)
            denominator += xDiff * xDiff
        }

        if (denominator == 0.0) return yMean.coerceAtLeast(0.0)

        val slope = numerator / denominator
        val intercept = yMean - slope * xMean

        return (slope * (n - 1 + monthsAhead) + intercept).coerceAtLeast(0.0)
    }

    /**
     * Forecasts spending per category. Each entry in [categoryHistory] maps a category name
     * to its monthly spending history.
     */
    fun forecastByCategory(
        categoryHistory: Map<String, List<MonthlySpending>>,
        monthsAhead: Int = 1,
    ): Map<String, Double> = categoryHistory.mapValues { (_, history) ->
        forecast(history, monthsAhead)
    }

    /**
     * Computes a confidence score (0.3–0.95) based on the quantity and stability of
     * spending data. Uses the coefficient of variation (std dev / mean) as the
     * primary signal — lower variance = higher confidence.
     */
    fun confidence(history: List<MonthlySpending>): Float {
        if (history.size < 2) return 0.3f
        val amounts = history.map { it.amount }
        val mean = amounts.average()
        if (mean == 0.0) return 0.3f

        val variance = amounts.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)
        val cv = stdDev / mean // coefficient of variation

        val baseConfidence = when {
            cv < 0.15 -> 0.9f  // very stable spending
            cv < 0.30 -> 0.75f
            cv < 0.50 -> 0.6f
            else -> 0.4f       // highly variable
        }

        // Bonus for more data points (up to +0.05 for 6+ months)
        val dataBonus = ((history.size - 2).coerceIn(0, 4) * 0.0125f)

        return (baseConfidence + dataBonus).coerceIn(0.3f, 0.95f)
    }
}
