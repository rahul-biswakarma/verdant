package com.verdant.core.model.repository

import com.verdant.core.model.AIInsight
import kotlinx.coroutines.flow.Flow

interface AIInsightRepository {
    suspend fun insert(insight: AIInsight)
    fun observeRecent(limit: Int): Flow<List<AIInsight>>
    suspend fun dismiss(id: String)
    suspend fun deleteExpired(now: Long)
    suspend fun deleteAll()
}
