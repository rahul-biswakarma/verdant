package com.verdant.core.model.repository

import com.verdant.core.model.Quest
import kotlinx.coroutines.flow.Flow

interface QuestRepository {
    fun observeActive(): Flow<List<Quest>>
    fun observeCompleted(): Flow<List<Quest>>
    suspend fun getById(id: String): Quest?
    suspend fun insert(quest: Quest)
    suspend fun update(quest: Quest)
    suspend fun start(id: String, startedAt: Long)
    suspend fun complete(id: String, completedAt: Long)
    suspend fun deleteExpired()
    suspend fun deleteAll()
}
