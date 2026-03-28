package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.database.dao.DeviceStatDao
import com.verdant.core.database.dao.EmotionalContextDao
import com.verdant.core.database.dao.HealthRecordDao
import com.verdant.core.database.dao.TransactionDao
import com.verdant.core.database.entity.EmotionalContextEntity
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.emotional.SignalFusionLayer
import com.verdant.core.model.DeviceStatType
import com.verdant.core.model.HealthRecordType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.util.UUID

@HiltWorker
class EmotionalEngineWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val emotionalContextDao: EmotionalContextDao,
    private val healthRecordDao: HealthRecordDao,
    private val deviceStatDao: DeviceStatDao,
    private val transactionDao: TransactionDao,
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_emotional_engine"
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    private val fusionLayer = SignalFusionLayer()

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val oneDayAgo = now - MILLIS_PER_DAY
            val today = LocalDate.now()

            // 1. Habit completion rate
            val habits = habitRepository.getAllHabits().filter { !it.isArchived }
            val totalScheduled = habits.size.coerceAtLeast(1)
            val entries = entryRepository.observeAllEntries(today, today).firstOrNull() ?: emptyList()
            val completedCount = entries.count { it.completed }
            val completionRate = completedCount.toFloat() / totalScheduled

            // 2. Health data: sleep + exercise
            val sleepRecord = healthRecordDao.getLatestByType(HealthRecordType.SLEEP.name)
            val sleepHours = sleepRecord?.value ?: 0.0

            val exerciseRecord = healthRecordDao.getLatestByType(HealthRecordType.EXERCISE.name)
            val exerciseMinutes = exerciseRecord?.value ?: 0.0

            // 3. Device stats: screen time, notifications, calendar
            val screenTimeStat = deviceStatDao.getLatestByType(DeviceStatType.SCREEN_TIME.name)
            val screenTimeMinutes = screenTimeStat?.value ?: 0.0

            val notificationStat = deviceStatDao.getLatestByType(DeviceStatType.NOTIFICATION_COUNT.name)
            val notificationCount = notificationStat?.value?.toInt() ?: 0

            val calendarStat = deviceStatDao.getLatestByType(DeviceStatType.CALENDAR_BUSY_HOURS.name)
            val calendarBusyHours = calendarStat?.value ?: 0.0

            // 4. Financial: spending ratio (today's spend vs 7-day average)
            val sevenDaysAgo = now - (7 * MILLIS_PER_DAY)
            val weeklySpend = transactionDao.totalSpent(sevenDaysAgo, now).firstOrNull() ?: 0.0
            val dailyAvg = if (weeklySpend > 0) weeklySpend / 7.0 else 0.0
            val todaySpend = transactionDao.totalSpent(oneDayAgo, now).firstOrNull() ?: 0.0
            val spendingRatio = if (dailyAvg > 0) todaySpend / dailyAvg else 0.0

            // 5. Fuse signals
            val signals = SignalFusionLayer.MicroSignals(
                habitCompletionRate = completionRate,
                screenTimeMinutes = screenTimeMinutes,
                sleepHours = sleepHours,
                exerciseMinutes = exerciseMinutes,
                spendingRatio = spendingRatio,
                notificationCount = notificationCount,
                calendarBusyHours = calendarBusyHours,
            )

            val result = fusionLayer.fuse(signals)

            // 6. Build contributing signals summary
            val contributingSignals = buildList {
                if (sleepHours > 0) add("sleep:${sleepHours}h")
                if (exerciseMinutes > 0) add("exercise:${exerciseMinutes}min")
                if (screenTimeMinutes > 0) add("screen:${screenTimeMinutes}min")
                if (notificationCount > 0) add("notifications:$notificationCount")
                if (calendarBusyHours > 0) add("calendar:${calendarBusyHours}h")
                add("habits:${(completionRate * 100).toInt()}%")
                if (todaySpend > 0) add("spending:${"%.0f".format(todaySpend)}")
            }

            // 7. Persist
            emotionalContextDao.insert(
                EmotionalContextEntity(
                    id = UUID.randomUUID().toString(),
                    date = now,
                    inferredMood = result.mood.name,
                    energyLevel = result.energyLevel,
                    confidence = result.confidence,
                    contributingSignals = contributingSignals.joinToString(","),
                    userConfirmed = false,
                )
            )

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
