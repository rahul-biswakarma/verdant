package com.verdant.core.model

data class PlayerProfile(
    val id: String,
    val level: Int,
    val title: String,
    val totalXP: Long,
    val currentLevelXP: Long,
    val xpToNextLevel: Long,
    val rank: PlayerRank,
    val stats: PlayerStats,
    val evolutionPath: EvolutionPath,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class PlayerRank {
    E, D, C, B, A, S;

    fun displayName(): String = when (this) {
        E -> "Awakened Novice"
        D -> "Rising Hunter"
        C -> "Shadow Striker"
        B -> "Elite Commander"
        A -> "Monarch's Will"
        S -> "Shadow Monarch"
    }
}

data class PlayerStats(
    val vitality: Int = 0,
    val discipline: Int = 0,
    val wisdom: Int = 0,
    val focus: Int = 0,
    val resilience: Int = 0,
    val awareness: Int = 0,
)

enum class EvolutionPath {
    VITALITY,
    WISDOM,
    MASTERY,
    BALANCE,
    SHADOW,
}
