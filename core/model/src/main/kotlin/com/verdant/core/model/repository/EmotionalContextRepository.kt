package com.verdant.core.model.repository

import com.verdant.core.model.EmotionalContext
import kotlinx.coroutines.flow.Flow

interface EmotionalContextRepository {
    suspend fun getLatest(): EmotionalContext?
    fun observeLatest(): Flow<EmotionalContext?>
    fun observeByRange(start: Long, end: Long): Flow<List<EmotionalContext>>
    suspend fun insert(context: EmotionalContext)
    suspend fun update(context: EmotionalContext)
    suspend fun deleteOlderThan(before: Long)
    suspend fun deleteAll()
}
