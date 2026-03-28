package com.verdant.core.model

data class HabitTargetHistory(
    val id: String,
    val habitId: String,
    val oldTarget: Double,
    val newTarget: Double,
    val changedAt: Long,
    val reason: String,
)
