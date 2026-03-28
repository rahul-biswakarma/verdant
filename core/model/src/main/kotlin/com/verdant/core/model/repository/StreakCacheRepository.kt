package com.verdant.core.model.repository

import com.verdant.core.model.StreakCache
import kotlinx.coroutines.flow.Flow

interface StreakCacheRepository {
    suspend fun upsert(cache: StreakCache)
    suspend fun getByHabitId(habitId: String): StreakCache?
    fun observeAll(): Flow<List<StreakCache>>
    suspend fun getAll(): List<StreakCache>
    suspend fun invalidate(habitId: String)
    suspend fun deleteAll()
}
