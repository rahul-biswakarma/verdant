package com.verdant.core.prediction

class FinancialHealthScorer {

    data class FinancialMetrics(
        val savingsRate: Double,
        val spendingVolatility: Double,
        val recurringRatio: Double,
        val categoryDiversity: Double,
    )

    fun score(metrics: FinancialMetrics): Int {
        val savingsScore = (metrics.savingsRate.coerceIn(0.0, 0.5) / 0.5 * 100).toInt()
        val volatilityScore = ((1.0 - metrics.spendingVolatility.coerceIn(0.0, 1.0)) * 100).toInt()
        val recurringScore = (metrics.recurringRatio.coerceIn(0.0, 1.0) * 100).toInt()
        val diversityScore = (metrics.categoryDiversity.coerceIn(0.0, 1.0) * 100).toInt()

        return ((savingsScore * 0.35 + volatilityScore * 0.25 + recurringScore * 0.20 + diversityScore * 0.20))
            .toInt()
            .coerceIn(0, 100)
    }
}
