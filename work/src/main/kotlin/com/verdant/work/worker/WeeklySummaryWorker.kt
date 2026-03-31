package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.ai.AIFeatureUnavailableException
import com.verdant.core.ai.VerdantAI
import com.verdant.core.ai.WeeklyReportData
import com.verdant.core.common.HabitDataAggregator
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.common.usecase.CalculateStreakUseCase
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.work.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Runs once a week (Sunday ~7 PM via WorkManager).
 *
 * 1. Gates on master toggle + weekly-summary toggle.
 * 2. Loads all active habits and the last 7 days of entries.
 * 3. Aggregates data via [HabitDataAggregator.aggregateForReport].
 * 4. Calls [VerdantAI.generateWeeklyReport] (Claude, requires network).
 *    Falls back to a plain-stats summary if offline or rate-limited.
 * 5. Posts a [NotificationHelper.postWeeklySummary] notification.
 * 6. Persists the summary as an [AIInsight] (7-day TTL).
 */
@HiltWorker
class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val verdantAI: VerdantAI,
    private val habitRepository: HabitRepository,
    private val habitEntryRepository: HabitEntryRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val aggregator: HabitDataAggregator,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.notificationsEnabled.first()) return Result.success()
        if (!prefs.weeklySummaryEnabled.first()) return Result.success()

        val today         = LocalDate.now()
        val weekStart     = today.with(DayOfWeek.MONDAY).minusWeeks(1)
        val weekEnd       = weekStart.plusDays(6)
        val habits = habitRepository.getAllHabits().filter { !it.isArchived }

        if (habits.isEmpty()) return Result.success()

        val habitIds  = habits.map { it.id }
        val streakMap = calculateStreak.currentStreaks(habitIds)

        // Load the past 7 days of entries
        val entries = habitEntryRepository
            .observeAllEntries(weekStart, weekEnd)
            .first()

        val aggregated = aggregator.aggregateForReport(
            habits     = habits,
            entries    = entries,
            streaks    = streakMap,
            today      = today,
            periodDays = 7,
        )

        val reportData = WeeklyReportData(
            weekStart      = weekStart,
            weekEnd        = weekEnd,
            aggregatedData = aggregated,
            habits         = habits,
        )

        val summary = runCatching { verdantAI.generateWeeklyReport(reportData) }
            .fold(
                onSuccess = { report ->
                    buildSummaryText(
                        report.summary,
                        aggregated.overallCompletionThisWeek,
                        aggregated.topStreaks.firstOrNull()?.let {
                            "${it.habitName} (${it.currentStreak} days)"
                        },
                    )
                },
                onFailure = { e ->
                    if (e is AIFeatureUnavailableException) {
                        buildFallbackSummary(aggregated.overallCompletionThisWeek, habits.size)
                    } else {
                        buildFallbackSummary(aggregated.overallCompletionThisWeek, habits.size)
                    }
                }
            )

        // Post notification
        NotificationHelper.postWeeklySummary(applicationContext, summary)

        return Result.success()
    }


    private fun buildSummaryText(
        aiSummary: String,
        completionRate: Float,
        topStreak: String?,
    ): String {
        val pct = (completionRate * 100).toInt()
        val streakLine = topStreak?.let { " Top streak: $it." } ?: ""
        return "$aiSummary ($pct% overall completion.$streakLine)"
    }

    private fun buildFallbackSummary(completionRate: Float, habitCount: Int): String {
        val pct   = (completionRate * 100).toInt()
        val emoji = when {
            pct >= 80 -> "🌟"
            pct >= 50 -> "💪"
            else      -> "🌱"
        }
        return "$emoji You completed $pct% of your $habitCount habits this week. " +
               "Keep building those positive routines!"
    }

    companion object {
        const val WORK_NAME = "verdant_weekly_summary"
    }
}
