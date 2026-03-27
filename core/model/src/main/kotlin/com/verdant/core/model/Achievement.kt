package com.verdant.core.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val unlockedAt: Long,
    val category: String,
)
