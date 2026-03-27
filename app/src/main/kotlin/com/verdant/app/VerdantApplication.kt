package com.verdant.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.verdant.work.notification.NotificationChannels
import com.verdant.work.worker.BudgetAlertWorker
import com.verdant.work.worker.DailyMotivationWorker
import com.verdant.work.worker.DeviceStatsSyncWorker
import com.verdant.work.worker.EmotionalEngineWorker
import com.verdant.work.worker.HealthSyncWorker
import com.verdant.work.worker.LifeScoreComputeWorker
import com.verdant.work.worker.PatternMiningWorker
import com.verdant.work.worker.PredictionWorker
import com.verdant.work.worker.QuestGeneratorWorker
import com.verdant.work.worker.RecurringTransactionDetectorWorker
import com.verdant.work.worker.SmsProcessingWorker
import com.verdant.work.worker.SpendingAlertWorker
import com.verdant.work.worker.StreakAlertWorker
import com.verdant.work.worker.WeatherSyncWorker
import com.verdant.work.worker.WeeklySummaryWorker
import com.verdant.work.worker.XPComputeWorker
import dagger.hilt.android.HiltAndroidApp
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class VerdantApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        NotificationChannels.createAll(this)
        scheduleDailyMotivation()
        scheduleStreakAlerts()
        scheduleWeeklySummary()
        scheduleSmsProcessing()
        scheduleSpendingAlerts()

        // Life Evolution Engine workers
        scheduleHealthSync()
        scheduleDeviceStatsSync()
        scheduleWeatherSync()
        scheduleEmotionalEngine()
        scheduleLifeScoreCompute()
        schedulePredictions()
        scheduleXPCompute()
        scheduleQuestGenerator()
        scheduleRecurringTransactionDetector()
        schedulePatternMining()
        scheduleBudgetAlerts()
    }


    /**
     * Enqueues a daily PeriodicWorkRequest that runs once every 24 h.
     * Initial delay is calculated to fire near 8 AM today (or 8 AM tomorrow
     * if it is already past 8 AM).
     *
     * [ExistingPeriodicWorkPolicy.KEEP] means re-installs (e.g. app updates) do
     * not reset an already-scheduled alarm.
     */
    private fun scheduleDailyMotivation() {
        val targetHour = 8
        val initialDelay = calculateInitialDelay(targetHour, 0)

        val request = PeriodicWorkRequestBuilder<DailyMotivationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyMotivationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Enqueues a PeriodicWorkRequest every 2 hours to check for at-risk streaks.
     * The worker itself gates on the 4 PM–10 PM window, so off-hours runs are
     * cheap no-ops.
     */
    private fun scheduleStreakAlerts() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<StreakAlertWorker>(
            repeatInterval = 2,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            StreakAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Enqueues a weekly PeriodicWorkRequest that fires on Sunday evenings at 7 PM.
     * Uses NETWORK_CONNECTED constraint since the worker calls Claude for richer
     * summaries (falls back to local stats if offline anyway).
     */
    private fun scheduleWeeklySummary() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val initialDelay = calculateInitialDelayForDayOfWeek(DayOfWeek.SUNDAY, 19, 0)

        val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(
            repeatInterval = 7,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WeeklySummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }


    /**
     * Reads new bank SMS every 2 hours and processes them into transactions.
     */
    private fun scheduleSmsProcessing() {
        val request = PeriodicWorkRequestBuilder<SmsProcessingWorker>(
            repeatInterval = 2,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SmsProcessingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * Checks for spending anomalies every 6 hours.
     */
    private fun scheduleSpendingAlerts() {
        val request = PeriodicWorkRequestBuilder<SpendingAlertWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder().setRequiresBatteryNotLow(true).build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SpendingAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Health data sync every 3 hours. */
    private fun scheduleHealthSync() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            repeatInterval = 3, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            HealthSyncWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Device stats sync every 6 hours. */
    private fun scheduleDeviceStatsSync() {
        val request = PeriodicWorkRequestBuilder<DeviceStatsSyncWorker>(
            repeatInterval = 6, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder().setRequiresBatteryNotLow(true).build()
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DeviceStatsSyncWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Weather sync every 12 hours. */
    private fun scheduleWeatherSync() {
        val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
            repeatInterval = 12, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WeatherSyncWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Emotional engine every 6 hours. */
    private fun scheduleEmotionalEngine() {
        val request = PeriodicWorkRequestBuilder<EmotionalEngineWorker>(
            repeatInterval = 6, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            EmotionalEngineWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Life score computation every 12 hours. */
    private fun scheduleLifeScoreCompute() {
        val request = PeriodicWorkRequestBuilder<LifeScoreComputeWorker>(
            repeatInterval = 12, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            LifeScoreComputeWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Run prediction models daily. */
    private fun schedulePredictions() {
        val request = PeriodicWorkRequestBuilder<PredictionWorker>(
            repeatInterval = 24, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder().setRequiresBatteryNotLow(true).build()
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PredictionWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** XP computation every 6 hours. */
    private fun scheduleXPCompute() {
        val request = PeriodicWorkRequestBuilder<XPComputeWorker>(
            repeatInterval = 6, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            XPComputeWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Quest generation daily. */
    private fun scheduleQuestGenerator() {
        val initialDelay = calculateInitialDelay(6, 0)
        val request = PeriodicWorkRequestBuilder<QuestGeneratorWorker>(
            repeatInterval = 24, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            QuestGeneratorWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Recurring transaction detection daily. */
    private fun scheduleRecurringTransactionDetector() {
        val request = PeriodicWorkRequestBuilder<RecurringTransactionDetectorWorker>(
            repeatInterval = 24, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RecurringTransactionDetectorWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Pattern mining weekly. */
    private fun schedulePatternMining() {
        val request = PeriodicWorkRequestBuilder<PatternMiningWorker>(
            repeatInterval = 7, repeatIntervalTimeUnit = TimeUnit.DAYS,
        ).setConstraints(
            Constraints.Builder().setRequiresBatteryNotLow(true).build()
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PatternMiningWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /** Budget alerts every 6 hours. */
    private fun scheduleBudgetAlerts() {
        val request = PeriodicWorkRequestBuilder<BudgetAlertWorker>(
            repeatInterval = 6, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BudgetAlertWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    /**
     * Returns milliseconds until the next occurrence of [hour]:[minute].
     * If that time has already passed today, returns delay until tomorrow.
     */
    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }

    /**
     * Returns milliseconds until the next [dayOfWeek] at [hour]:[minute].
     */
    private fun calculateInitialDelayForDayOfWeek(
        dayOfWeek: DayOfWeek,
        hour: Int,
        minute: Int,
    ): Long {
        val now = ZonedDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            .with(TemporalAdjusters.nextOrSame(dayOfWeek))
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }
}
