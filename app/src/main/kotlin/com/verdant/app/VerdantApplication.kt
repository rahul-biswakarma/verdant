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
import com.verdant.work.worker.DailyMotivationWorker
import com.verdant.work.worker.StreakAlertWorker
import com.verdant.work.worker.WeeklySummaryWorker
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

        // Create all notification channels (idempotent — safe to call on every launch)
        NotificationChannels.createAll(this)

        // Schedule background workers
        scheduleDailyMotivation()
        scheduleStreakAlerts()
        scheduleWeeklySummary()
    }

    // ── Worker scheduling ─────────────────────────────────────────────────────

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

    // ── Helpers ───────────────────────────────────────────────────────────────

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
