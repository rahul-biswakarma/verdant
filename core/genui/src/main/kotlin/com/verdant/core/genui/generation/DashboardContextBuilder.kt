package com.verdant.core.genui.generation

import com.verdant.core.model.repository.EmotionalContextRepository
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.model.repository.HealthRecordRepository
import com.verdant.core.model.repository.PlayerProfileRepository
import com.verdant.core.model.repository.StreakCacheRepository
import com.verdant.core.model.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects user data into a compact [DashboardGenerationContext] for the LLM.
 */
@Singleton
class DashboardContextBuilder @Inject constructor(
    private val habitRepository: HabitRepository,
    private val habitEntryRepository: HabitEntryRepository,
    private val streakCacheRepository: StreakCacheRepository,
    private val transactionRepository: TransactionRepository,
    private val healthRecordRepository: HealthRecordRepository,
    private val emotionalContextRepository: EmotionalContextRepository,
    private val playerProfileRepository: PlayerProfileRepository,
) {

    suspend fun build(): DashboardGenerationContext {
        val today = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY)
        val zone = ZoneId.systemDefault()

        // Habits
        val habits = habitRepository.getAllHabits()
        val todayEntries = habitEntryRepository.observeAllEntries(today, today).first()
        val weekEntries = habitEntryRepository.observeAllEntries(weekStart, today).first()
        val streaks = streakCacheRepository.getAll()

        val todayCompleted = todayEntries.count { it.completed }
        val todayTotal = habits.size
        val weekCompleted = weekEntries.count { it.completed }
        val weekTotal = habits.size * (today.toEpochDay() - weekStart.toEpochDay() + 1).toInt()

        val bestStreak = streaks.maxOfOrNull { it.currentStreak } ?: 0
        val activeStreaks = streaks.count { it.currentStreak > 0 }

        val topHabits = habits.take(5).map { habit ->
            val streak = streaks.find { it.habitId == habit.id }
            HabitSummaryItem(
                name = habit.name,
                completionRate = streak?.completionRate ?: 0f,
                currentStreak = streak?.currentStreak ?: 0,
            )
        }

        // Finance
        val monthStart = today.withDayOfMonth(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = today.plusMonths(1).withDayOfMonth(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val spent = transactionRepository.totalSpent(monthStart, monthEnd).first() ?: 0.0
        val income = transactionRepository.totalIncome(monthStart, monthEnd).first() ?: 0.0

        // Health
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1_000L
        val healthRecords = healthRecordRepository.observeByRange(sevenDaysAgo, now).first()
        val steps = healthRecords.filter { it.recordType.name == "STEPS" }
        val sleep = healthRecords.filter { it.recordType.name == "SLEEP" }

        // Emotional
        val emotionalCtx = emotionalContextRepository.getLatest()

        // Player
        val profile = playerProfileRepository.get()

        val hour = LocalTime.now().hour
        val timeOfDay = when {
            hour < 12 -> "morning"
            hour < 17 -> "afternoon"
            else -> "evening"
        }

        return DashboardGenerationContext(
            habitData = HabitDataSummary(
                totalHabits = todayTotal,
                completionRateToday = if (todayTotal > 0) todayCompleted.toFloat() / todayTotal else 0f,
                completionRateWeek = if (weekTotal > 0) weekCompleted.toFloat() / weekTotal else 0f,
                bestStreak = bestStreak,
                activeStreaks = activeStreaks,
                topHabits = topHabits,
            ),
            financeSummary = FinanceSummary(
                monthlySpent = spent,
                monthlyIncome = income,
                hasTransactions = spent > 0 || income > 0,
            ),
            healthSummary = HealthSummary(
                avgSteps = if (steps.isNotEmpty()) steps.map { it.value }.average() else 0.0,
                avgSleepHours = if (sleep.isNotEmpty()) sleep.map { it.value }.average() else 0.0,
                hasHealthData = healthRecords.isNotEmpty(),
            ),
            emotionalSummary = EmotionalSummary(
                latestMood = emotionalCtx?.inferredMood?.name,
                energyLevel = emotionalCtx?.energyLevel,
            ),
            playerSummary = profile?.let {
                PlayerSummary(
                    level = it.level,
                    totalXP = it.totalXP,
                    rank = it.rank.name,
                )
            },
            dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            timeOfDay = timeOfDay,
        )
    }
}
