package com.verdant.core.supabase.dto

import com.verdant.core.model.Quest
import com.verdant.core.model.QuestDifficulty
import com.verdant.core.model.QuestStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val description: String,
    val difficulty: String,
    @SerialName("xp_reward") val xpReward: Int,
    val conditions: String,
    @SerialName("time_limit") val timeLimit: Long? = null,
    @SerialName("generated_by") val generatedBy: String,
    val reasoning: String,
    val status: String,
    @SerialName("started_at") val startedAt: Long? = null,
    @SerialName("completed_at") val completedAt: Long? = null,
)

fun QuestDto.toDomain(): Quest = Quest(
    id = id,
    title = title,
    description = description,
    difficulty = QuestDifficulty.valueOf(difficulty),
    xpReward = xpReward,
    conditions = conditions,
    timeLimit = timeLimit,
    generatedBy = generatedBy,
    reasoning = reasoning,
    status = QuestStatus.valueOf(status),
    startedAt = startedAt,
    completedAt = completedAt,
)

fun Quest.toDto(userId: String): QuestDto = QuestDto(
    id = id,
    userId = userId,
    title = title,
    description = description,
    difficulty = difficulty.name,
    xpReward = xpReward,
    conditions = conditions,
    timeLimit = timeLimit,
    generatedBy = generatedBy,
    reasoning = reasoning,
    status = status.name,
    startedAt = startedAt,
    completedAt = completedAt,
)
