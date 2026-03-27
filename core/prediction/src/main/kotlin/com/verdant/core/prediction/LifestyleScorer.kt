package com.verdant.core.prediction

class LifestyleScorer {

    data class DimensionScores(
        val health: Int,
        val financial: Int,
        val productivity: Int,
        val wellness: Int,
    )

    fun score(dimensions: DimensionScores): Int {
        return (dimensions.health * 0.30 +
                dimensions.financial * 0.25 +
                dimensions.productivity * 0.25 +
                dimensions.wellness * 0.20)
            .toInt()
            .coerceIn(0, 100)
    }
}
