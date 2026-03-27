package com.verdant.core.model

data class CrossCorrelation(
    val id: String,
    val dimensionA: String,
    val dimensionB: String,
    val correlationStrength: Float,
    val description: String,
    val discoveredAt: Long,
    val sampleSize: Int,
)
