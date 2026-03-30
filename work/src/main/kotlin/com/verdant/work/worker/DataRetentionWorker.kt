package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.model.repository.ActivityRecordRepository
import com.verdant.core.model.repository.AIInsightRepository
import com.verdant.core.model.repository.DeviceSignalRepository
import com.verdant.core.model.repository.DeviceStatRepository
import com.verdant.core.model.repository.EmotionalContextRepository
import com.verdant.core.model.repository.HealthRecordRepository
import com.verdant.core.model.repository.LifeScoreRepository
import com.verdant.core.model.repository.PendingAIRequestRepository
import com.verdant.core.model.repository.PredictionRepository
import com.verdant.core.model.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class DataRetentionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val activityRecordRepository: ActivityRecordRepository,
    private val deviceStatRepository: DeviceStatRepository,
    private val healthRecordRepository: HealthRecordRepository,
    private val weatherRepository: WeatherRepository,
    private val emotionalContextRepository: EmotionalContextRepository,
    private val lifeScoreRepository: LifeScoreRepository,
    private val predictionRepository: PredictionRepository,
    private val aiInsightRepository: AIInsightRepository,
    private val deviceSignalRepository: DeviceSignalRepository,
    private val pendingAIRequestRepository: PendingAIRequestRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val now = System.currentTimeMillis()

            activityRecordRepository.deleteOlderThan(now - NINETY_DAYS_MS)
            healthRecordRepository.deleteOlderThan(now - NINETY_DAYS_MS)
            lifeScoreRepository.deleteOlderThan(now - NINETY_DAYS_MS)
            emotionalContextRepository.deleteOlderThan(now - SIXTY_DAYS_MS)
            deviceStatRepository.deleteOlderThan(now - THIRTY_DAYS_MS)
            deviceSignalRepository.deleteOlderThan(now - THIRTY_DAYS_MS)
            weatherRepository.deleteOlderThan(now - SEVEN_DAYS_MS)
            pendingAIRequestRepository.deleteOlderThan(now - ONE_DAY_MS)
            predictionRepository.deleteExpired(now)
            aiInsightRepository.deleteExpired(now)

            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "verdant_data_retention"

        private val ONE_DAY_MS = TimeUnit.DAYS.toMillis(1)
        private val SEVEN_DAYS_MS = TimeUnit.DAYS.toMillis(7)
        private val THIRTY_DAYS_MS = TimeUnit.DAYS.toMillis(30)
        private val SIXTY_DAYS_MS = TimeUnit.DAYS.toMillis(60)
        private val NINETY_DAYS_MS = TimeUnit.DAYS.toMillis(90)
    }
}
