package com.verdant.core.model

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: QuestDifficulty,
    val xpReward: Int,
    val conditions: String,
    val timeLimit: Long?,
    val generatedBy: String,
    val reasoning: String,
    val status: QuestStatus,
    val startedAt: Long?,
    val completedAt: Long?,
)

enum class QuestDifficulty {
    DAILY,
    WEEKLY,
    EPIC,
    LEGENDARY,
}

enum class QuestStatus {
    AVAILABLE,
    ACTIVE,
    COMPLETED,
    EXPIRED,
    FAILED,
}
