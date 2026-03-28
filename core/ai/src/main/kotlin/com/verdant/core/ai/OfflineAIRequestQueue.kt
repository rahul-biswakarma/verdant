package com.verdant.core.ai

import com.verdant.core.model.PendingAIRequest
import com.verdant.core.model.repository.PendingAIRequestRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queues AI requests that fail due to network issues for later retry.
 */
@Singleton
class OfflineAIRequestQueue @Inject constructor(
    private val pendingAIRequestRepository: PendingAIRequestRepository,
) {
    /** Queues a failed request for later retry. */
    suspend fun enqueue(requestType: String, payload: String) {
        pendingAIRequestRepository.insert(
            PendingAIRequest(
                id = UUID.randomUUID().toString(),
                requestType = requestType,
                payload = payload,
                createdAt = System.currentTimeMillis(),
                attemptCount = 0,
            )
        )
    }

    /** Gets all pending requests that haven't exceeded max retries. */
    suspend fun getPending(maxRetries: Int = 3): List<PendingAIRequest> =
        pendingAIRequestRepository.getPending(maxRetries)

    /** Marks a request as attempted (increments attempt count). */
    suspend fun markAttempted(id: String) {
        pendingAIRequestRepository.incrementAttempt(id)
    }

    /** Removes a successfully processed request. */
    suspend fun remove(id: String) {
        pendingAIRequestRepository.delete(id)
    }

    /** Cleans up expired requests older than 24 hours. */
    suspend fun cleanup() {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        pendingAIRequestRepository.deleteOlderThan(cutoff)
    }
}
