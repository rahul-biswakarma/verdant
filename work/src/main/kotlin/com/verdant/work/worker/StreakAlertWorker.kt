package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.ai.NudgeContext
import com.verdant.core.ai.VerdantAI
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.datastore.NudgeTone
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.work.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Runs every 2 hours during the 4 PM–10 PM active window.
 *
 * Algorithm:
 *  1. Gate: master toggle + streak-alerts toggle + quiet hours.
 *  2. Only proceed between 4 PM–10 PM.
 *  3. Load all active habits; compute current streaks.
 *  4. Filter to habits with streak ≥ 3 that are NOT completed today.
 *  5. Respect [maxNudgesPerDay] cap (take top N by streak length).
 *  6. For each, generate nudge via [VerdantAI.generateNudge] and post it.
 */
@HiltWorker
class StreakAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val verdantAI: VerdantAI,
    private val habitDao: HabitDao,
    private val habitEntryRepository: HabitEntryRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.notificationsEnabled.first()) return Result.success()
        if (!prefs.streakAlertsEnabled.first()) return Result.success()

        val now = LocalTime.now()

        // Only nudge between 4 PM and 10 PM
        if (now.hour < NUDGE_WINDOW_START_HOUR || now.hour >= NUDGE_WINDOW_END_HOUR) {
            return Result.success()
        }

        // Quiet-hours gate
        val quietStart = prefs.quietHoursStart.first()
        val quietEnd   = prefs.quietHoursEnd.first()
        if (isInQuietHours(now.hour, quietStart, quietEnd)) return Result.success()

        val today         = LocalDate.now()
        val habitEntities = habitDao.getAll().filter { !it.isArchived }
        if (habitEntities.isEmpty()) return Result.success()

        val habits    = habitEntities.map { it.toDomain() }
        val streakMap = calculateStreak.currentStreaks(habits.map { it.id })
        val maxNudges = prefs.maxNudgesPerDay.first()
        val nudgeTone = NudgeTone.fromKey(prefs.nudgeTone.first())
        val timeStr   = now.format(DateTimeFormatter.ofPattern("HH:mm"))

        // Habits with an at-risk streak (≥ MIN_STREAK) that haven't been done today
        val atRiskHabits = habits
            .filter { habit ->
                val streak = streakMap[habit.id] ?: 0
                streak >= MIN_STREAK_FOR_NUDGE &&
                    habitEntryRepository.getByHabitAndDate(habit.id, today)?.completed != true
            }
            .sortedByDescending { streakMap[it.id] ?: 0 } // highest streak first
            .take(maxNudges)

        for (habit in atRiskHabits) {
            val streak   = streakMap[habit.id] ?: 0
            val nudgeCtx = NudgeContext(
                habit               = habit,
                currentStreak       = streak,
                usualCompletionTime = null, // Could be persisted in DataStore per-habit in future
                currentTime         = timeStr,
            )
            val nudgeText = runCatching { verdantAI.generateNudge(nudgeCtx) }
                .getOrDefault(defaultNudge(habit.name, streak, nudgeTone))

            NotificationHelper.postStreakNudge(
                context    = applicationContext,
                habitId    = habit.id,
                habitName  = habit.name,
                streakDays = streak,
                nudgeText  = nudgeText,
            )
        }

        return Result.success()
    }


    /**
     * Returns true when [hour] falls inside the quiet window.
     * Handles overnight windows (e.g. quiet start=22, end=7 crosses midnight).
     */
    private fun isInQuietHours(hour: Int, start: Int, end: Int): Boolean =
        if (start <= end) hour in start until end
        else hour >= start || hour < end

    private fun defaultNudge(name: String, streak: Int, tone: NudgeTone): String = when (tone) {
        NudgeTone.GENTLE     -> "Hey! Don't forget about $name — you've got a $streak-day streak 🌱"
        NudgeTone.MOTIVATING -> "Your $streak-day $name streak is waiting — finish strong today! 🔥"
        NudgeTone.DIRECT     -> "Log $name now to keep your $streak-day streak alive."
    }

    companion object {
        const val WORK_NAME = "verdant_streak_alert"

        private const val NUDGE_WINDOW_START_HOUR = 16 // 4 PM
        private const val NUDGE_WINDOW_END_HOUR   = 22 // 10 PM
        private const val MIN_STREAK_FOR_NUDGE    = 3
    }
}
