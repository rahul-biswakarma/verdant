package com.verdant.core.supabase.dto

import com.verdant.core.model.EvolutionPath
import com.verdant.core.model.PlayerProfile
import com.verdant.core.model.PlayerRank
import com.verdant.core.model.PlayerStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfileDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val level: Int,
    val title: String,
    @SerialName("total_xp") val totalXP: Long,
    @SerialName("current_level_xp") val currentLevelXP: Long,
    @SerialName("xp_to_next_level") val xpToNextLevel: Long,
    val rank: String,
    val stats: String,
    @SerialName("evolution_path") val evolutionPath: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

fun PlayerProfileDto.toDomain(): PlayerProfile {
    val statParts = stats.split(",").map { it.toIntOrNull() ?: 0 }
    return PlayerProfile(
        id = id,
        level = level,
        title = title,
        totalXP = totalXP,
        currentLevelXP = currentLevelXP,
        xpToNextLevel = xpToNextLevel,
        rank = PlayerRank.valueOf(rank),
        stats = PlayerStats(
            vitality = statParts.getOrElse(0) { 0 },
            discipline = statParts.getOrElse(1) { 0 },
            wisdom = statParts.getOrElse(2) { 0 },
            focus = statParts.getOrElse(3) { 0 },
            resilience = statParts.getOrElse(4) { 0 },
            awareness = statParts.getOrElse(5) { 0 },
        ),
        evolutionPath = EvolutionPath.valueOf(evolutionPath),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun PlayerProfile.toDto(userId: String): PlayerProfileDto = PlayerProfileDto(
    id = id,
    userId = userId,
    level = level,
    title = title,
    totalXP = totalXP,
    currentLevelXP = currentLevelXP,
    xpToNextLevel = xpToNextLevel,
    rank = rank.name,
    stats = "${stats.vitality},${stats.discipline},${stats.wisdom},${stats.focus},${stats.resilience},${stats.awareness}",
    evolutionPath = evolutionPath.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
