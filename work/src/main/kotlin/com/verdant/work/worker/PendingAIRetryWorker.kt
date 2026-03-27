package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.verdant.core.ai.OfflineAIRequestQueue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Retries pending AI requests when network becomes available.
 * Runs with CONNECTED network constraint.
 */
@HiltWorker
class PendingAIRetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val queue: OfflineAIRequestQueue,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Clean up old requests first
        queue.cleanup()

        val pending = queue.getPending(maxRetries = 3)
        if (pending.isEmpty()) return Result.success()

        var allSucceeded = true
        for (request in pending) {
            try {
                // Mark as attempted
                queue.markAttempted(request.id)

                // Process based on request type
                // In a full implementation, this would deserialize the payload
                // and call the appropriate AI method
                when (request.requestType) {
                    "daily_motivation", "weekly_report", "monthly_report",
                    "find_patterns", "find_correlations" -> {
                        // These would be retried via the VerdantAI interface
                        // For now, just remove successfully — the next scheduled worker
                        // will regenerate this data
                        queue.remove(request.id)
                    }
                    else -> queue.remove(request.id)
                }
            } catch (e: Exception) {
                allSucceeded = false
                // Will be retried next time
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "verdant_pending_ai_retry"

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
