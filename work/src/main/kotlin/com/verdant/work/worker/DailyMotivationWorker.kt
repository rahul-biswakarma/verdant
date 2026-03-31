package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.ai.MotivationContext
import com.verdant.core.ai.VerdantAI
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.common.usecase.CalculateStreakUseCase
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.work.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Runs once a day (scheduled at ~8 AM via WorkManager).
 *
 * 1. Gates on master toggle + daily-motivation toggle.
 * 2. Collects today's scheduled habits and active streaks.
 * 3. Asks [VerdantAI.generateMotivation] for a personalised message
 *    (the router automatically tries Gemini Nano and Claude concurrently).
 * 4. Posts a [NotificationHelper.postDailyMotivation] notification.
 * 5. Persists the message as an [AIInsight] for the Insights feed (48 h TTL).
 * 6. Fires milestone notifications for any habit that crossed a key streak threshold today.
 */
@HiltWorker
class DailyMotivationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val verdantAI: VerdantAI,
    private val habitRepository: HabitRepository,
    private val habitEntryRepository: HabitEntryRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.notificationsEnabled.first()) return Result.success()
        if (!prefs.dailyMotivationEnabled.first()) return Result.success()

        val today         = LocalDate.now()
        val habits = habitRepository.getAllHabits().filter { !it.isArchived }
        if (habits.isEmpty()) return Result.success()

        val habitIds  = habits.map { it.id }
        val streakMap = calculateStreak.currentStreaks(habitIds)

        // Today's scheduled habits (bit mask: Mon=1 << 0 … Sun=1 << 6)
        val todayBit = 1 shl (today.dayOfWeek.value - 1)
        val todayHabits = habits.filter { (it.scheduleDays and todayBit) != 0 }

        // Yesterday's completion fraction
        val yesterday          = today.minusDays(1)
        val yesterdayCompleted = habits.count { h ->
            habitEntryRepository.getByHabitAndDate(h.id, yesterday)?.completed == true
        }
        val yesterdayFraction  = if (habits.isEmpty()) 0f
                                 else yesterdayCompleted.toFloat() / habits.size

        // This week's completion fraction (last 7 days)
        val weekFraction = calculateWeekCompletion(habitIds, today)

        val motivationCtx = MotivationContext(
            todayHabits         = todayHabits,
            activeStreaks        = streakMap.filterValues { it > 0 },
            yesterdayCompletion = yesterdayFraction,
            weekCompletion      = weekFraction,
        )

        val message = runCatching { verdantAI.generateMotivation(motivationCtx) }
            .getOrDefault("Great habits start with great mornings. Let's go! 🌱")

        // Post notification
        NotificationHelper.postDailyMotivation(applicationContext, message)

        // Milestone check
        checkMilestones(habits.map { it.id to it.name }, streakMap)

        return Result.success()
    }


    private suspend fun calculateWeekCompletion(habitIds: List<String>, today: LocalDate): Float {
        if (habitIds.isEmpty()) return 0f
        var completed = 0
        for (id in habitIds) {
            for (offset in 0 until 7) {
                val date = today.minusDays(offset.toLong())
                if (habitEntryRepository.getByHabitAndDate(id, date)?.completed == true) {
                    completed++
                }
            }
        }
        return completed.toFloat() / (habitIds.size * 7)
    }

    private fun checkMilestones(habits: List<Pair<String, String>>, streakMap: Map<String, Int>) {
        val milestones = setOf(7, 14, 21, 30, 60, 100, 180, 365)
        for ((id, name) in habits) {
            val streak = streakMap[id] ?: continue
            if (streak in milestones) {
                NotificationHelper.postMilestone(applicationContext, id, name, streak)
            }
        }
    }

    companion object {
        const val WORK_NAME = "verdant_daily_motivation"
    }
}
