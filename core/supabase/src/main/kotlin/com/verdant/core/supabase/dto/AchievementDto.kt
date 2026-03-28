package com.verdant.core.supabase.dto

import com.verdant.core.model.Achievement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AchievementDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val description: String,
    @SerialName("xp_reward") val xpReward: Int,
    @SerialName("unlocked_at") val unlockedAt: Long,
    val category: String,
)

fun AchievementDto.toDomain(): Achievement = Achievement(
    id = id,
    title = title,
    description = description,
    xpReward = xpReward,
    unlockedAt = unlockedAt,
    category = category,
)

fun Achievement.toDto(userId: String): AchievementDto = AchievementDto(
    id = id,
    userId = userId,
    title = title,
    description = description,
    xpReward = xpReward,
    unlockedAt = unlockedAt,
    category = category,
)
