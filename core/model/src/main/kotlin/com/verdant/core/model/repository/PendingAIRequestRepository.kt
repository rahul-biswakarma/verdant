package com.verdant.core.model.repository

import com.verdant.core.model.PendingAIRequest

interface PendingAIRequestRepository {
    suspend fun insert(request: PendingAIRequest)
    suspend fun getAll(): List<PendingAIRequest>
    suspend fun incrementAttempt(id: String)
    suspend fun delete(id: String)
    suspend fun getPending(maxRetries: Int): List<PendingAIRequest>
    suspend fun deleteFailedRequests(maxAttempts: Int)
    suspend fun deleteOlderThan(cutoff: Long)
    suspend fun deleteAll()
}
