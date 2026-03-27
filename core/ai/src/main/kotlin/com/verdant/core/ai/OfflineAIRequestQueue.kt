package com.verdant.core.ai

import com.verdant.core.database.dao.PendingAIRequestDao
import com.verdant.core.database.entity.PendingAIRequestEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queues AI requests that fail due to network issues for later retry.
 */
@Singleton
class OfflineAIRequestQueue @Inject constructor(
    private val pendingDao: PendingAIRequestDao,
) {
    /** Queues a failed request for later retry. */
    suspend fun enqueue(requestType: String, payload: String) {
        pendingDao.insert(
            PendingAIRequestEntity(
                id = UUID.randomUUID().toString(),
                requestType = requestType,
                payload = payload,
                createdAt = System.currentTimeMillis(),
                attemptCount = 0,
            )
        )
    }

    /** Gets all pending requests that haven't exceeded max retries. */
    suspend fun getPending(maxRetries: Int = 3): List<PendingAIRequestEntity> =
        pendingDao.getPending(maxRetries)

    /** Marks a request as attempted (increments attempt count). */
    suspend fun markAttempted(id: String) {
        pendingDao.incrementAttempt(id)
    }

    /** Removes a successfully processed request. */
    suspend fun remove(id: String) {
        pendingDao.delete(id)
    }

    /** Cleans up expired requests older than 24 hours. */
    suspend fun cleanup() {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        pendingDao.deleteOlderThan(cutoff)
    }
}
