package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.model.LifeScore
import com.verdant.core.model.ScoreType
import com.verdant.core.model.repository.LifeScoreRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

@HiltWorker
class LifeScoreComputeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val lifeScoreRepository: LifeScoreRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_life_score_compute"
    }

    override suspend fun doWork(): Result {
        // TODO: Run all scorers (FinancialHealthScorer, LifestyleScorer, etc.)
        // and persist results to life_scores table
        val now = System.currentTimeMillis()
        val scores = listOf(ScoreType.HEALTH, ScoreType.FINANCIAL, ScoreType.PRODUCTIVITY, ScoreType.WELLNESS, ScoreType.LIFESTYLE, ScoreType.STRESS)
            .map { type ->
                LifeScore(
                    id = UUID.randomUUID().toString(),
                    scoreType = type,
                    score = 50,
                    components = "{}",
                    computedDate = now,
                    createdAt = now,
                )
            }
        lifeScoreRepository.insertAll(scores)
        return Result.success()
    }
}
