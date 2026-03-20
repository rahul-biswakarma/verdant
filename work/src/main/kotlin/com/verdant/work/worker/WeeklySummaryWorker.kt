package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.ai.AIFeatureUnavailableException
import com.verdant.core.ai.VerdantAI
import com.verdant.core.ai.WeeklyReportData
import com.verdant.core.common.HabitDataAggregator
import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.entity.AIInsightEntity
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.model.InsightType
import com.verdant.work.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

/**
 * Runs once a week (Sunday ~7 PM via WorkManager).
 *
 * 1. Gates on master toggle + weekly-summary toggle.
 * 2. Loads all active habits and the last 7 days of entries.
 * 3. Aggregates data via [HabitDataAggregator.aggregateForReport].
 * 4. Calls [VerdantAI.generateWeeklyReport] (Claude, requires network).
 *    Falls back to a plain-stats summary if offline or rate-limited.
 * 5. Posts a [NotificationHelper.postWeeklySummary] notification.
 * 6. Persists the summary as an [AIInsightEntity] (7-day TTL).
 */
@HiltWorker
class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val verdantAI: VerdantAI,
    private val habitDao: HabitDao,
    private val habitEntryRepository: HabitEntryRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val aggregator: HabitDataAggregator,
    private val insightDao: AIInsightDao,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.notificationsEnabled.first()) return Result.success()
        if (!prefs.weeklySummaryEnabled.first()) return Result.success()

        val today         = LocalDate.now()
        val weekStart     = today.with(DayOfWeek.MONDAY).minusWeeks(1)
        val weekEnd       = weekStart.plusDays(6)
        val habitEntities = habitDao.getAll().filter { !it.isArchived }

        if (habitEntities.isEmpty()) return Result.success()

        val habits    = habitEntities.map { it.toDomain() }
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

        // Persist to AI Insights feed (7-day TTL)
        val now = System.currentTimeMillis()
        insightDao.insert(
            AIInsightEntity(
                id              = UUID.randomUUID().toString(),
                type            = InsightType.WEEKLY_SUMMARY,
                content         = summary,
                relatedHabitIds = emptyList(),
                generatedAt     = now,
                expiresAt       = now + 7 * 24 * 60 * 60 * 1_000L,
                dismissed       = false,
            )
        )

        return Result.success()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
