package com.verdant.core.model

data class AIInsight(
    val id: String,
    val type: InsightType,
    val content: String,
    val relatedHabitIds: List<String>,
    val generatedAt: Long,
    val expiresAt: Long,
    val dismissed: Boolean,
)
