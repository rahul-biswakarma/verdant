package com.verdant.core.prediction

class SpendingForecaster {

    data class MonthlySpending(val month: Long, val amount: Double)

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

        if (denominator == 0.0) return yMean

        val slope = numerator / denominator
        val intercept = yMean - slope * xMean

        return slope * (n - 1 + monthsAhead) + intercept
    }
}
