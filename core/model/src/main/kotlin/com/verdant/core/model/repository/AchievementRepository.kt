package com.verdant.core.model.repository

import com.verdant.core.model.Achievement
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    suspend fun insert(achievement: Achievement)
    fun observeAll(): Flow<List<Achievement>>
    suspend fun getById(id: String): Achievement?
    suspend fun count(): Int
    suspend fun totalXPFromAchievements(): Long?
    suspend fun deleteAll()
}
