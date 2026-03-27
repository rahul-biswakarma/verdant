package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.database.dao.LifeScoreDao
import com.verdant.core.database.entity.LifeScoreEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

@HiltWorker
class LifeScoreComputeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val lifeScoreDao: LifeScoreDao,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_life_score_compute"
    }

    override suspend fun doWork(): Result {
        // TODO: Run all scorers (FinancialHealthScorer, LifestyleScorer, etc.)
        // and persist results to life_scores table
        val now = System.currentTimeMillis()
        val scores = listOf("HEALTH", "FINANCIAL", "PRODUCTIVITY", "WELLNESS", "LIFESTYLE", "STRESS")
        scores.forEach { type ->
            lifeScoreDao.insert(
                LifeScoreEntity(
                    id = UUID.randomUUID().toString(),
                    scoreType = type,
                    score = 50,
                    components = "{}",
                    computedDate = now,
                    createdAt = now,
                )
            )
        }
        return Result.success()
    }
}
