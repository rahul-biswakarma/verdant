package com.verdant.core.model

data class HabitRiskSnapshot(
    val id: String,
    val habitId: String,
    val score: Double,
    val computedAt: Long,
    val triggeringFactors: String,
)
