package com.verdant.core.model

data class LifeScore(
    val id: String,
    val scoreType: ScoreType,
    val score: Int,
    val components: String,
    val computedDate: Long,
    val createdAt: Long,
)

enum class ScoreType {
    HEALTH,
    FINANCIAL,
    PRODUCTIVITY,
    WELLNESS,
    LIFESTYLE,
    STRESS,
}
