package com.verdant.core.prediction

class HealthTrajectoryPredictor {

    data class DataPoint(val timestamp: Long, val value: Double)

    fun predict(data: List<DataPoint>, daysAhead: Int = 7): Double {
        if (data.isEmpty()) return 0.0
        if (data.size == 1) return data.first().value

        val weights = data.mapIndexed { index, _ ->
            val recency = (index + 1).toDouble() / data.size
            recency * recency
        }
        val totalWeight = weights.sum()

        val weightedAvg = data.zip(weights).sumOf { (dp, w) -> dp.value * w } / totalWeight

        val n = data.size
        if (n < 3) return weightedAvg

        val recentSlope = (data.last().value - data[n - 3].value) / 2.0
        return weightedAvg + recentSlope * daysAhead * 0.3
    }
}
