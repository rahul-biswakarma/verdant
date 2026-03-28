package com.verdant.core.supabase.dto

import com.verdant.core.model.LifeScore
import com.verdant.core.model.ScoreType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LifeScoreDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("score_type") val scoreType: String,
    val score: Int,
    val components: String,
    @SerialName("computed_date") val computedDate: Long,
    @SerialName("created_at") val createdAt: Long,
)

fun LifeScoreDto.toDomain(): LifeScore = LifeScore(
    id = id,
    scoreType = ScoreType.valueOf(scoreType),
    score = score,
    components = components,
    computedDate = computedDate,
    createdAt = createdAt,
)

fun LifeScore.toDto(userId: String): LifeScoreDto = LifeScoreDto(
    id = id,
    userId = userId,
    scoreType = scoreType.name,
    score = score,
    components = components,
    computedDate = computedDate,
    createdAt = createdAt,
)
