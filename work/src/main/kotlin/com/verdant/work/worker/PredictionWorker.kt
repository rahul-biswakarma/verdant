package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.model.repository.PredictionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PredictionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val predictionRepository: PredictionRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_predictions"
    }

    override suspend fun doWork(): Result {
        // TODO: Run SpendingForecaster, HabitSustainabilityScorer, HealthTrajectoryPredictor
        // Persist results to predictions table
        predictionRepository.deleteExpired(System.currentTimeMillis())
        return Result.success()
    }
}
