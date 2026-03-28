package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.model.Quest
import com.verdant.core.model.repository.EmotionalContextRepository
import com.verdant.core.model.repository.QuestRepository
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.common.usecase.CalculateStreakUseCase
import com.verdant.core.model.QuestDifficulty
import com.verdant.core.model.QuestStatus
import com.verdant.core.model.TrackingType
import com.verdant.core.model.isScheduledForDate
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.util.UUID

@HiltWorker
class QuestGeneratorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val questRepository: QuestRepository,
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val emotionalContextRepository: EmotionalContextRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_quest_generator"
        private const val MAX_ACTIVE_QUESTS = 3
    }

    override suspend fun doWork(): Result {
        return try {
            questRepository.deleteExpired()

            // Don't generate if user already has enough quests
            val activeQuests = questRepository.observeActive().firstOrNull() ?: emptyList()
            if (activeQuests.size >= MAX_ACTIVE_QUESTS) return Result.success()

            val habits = habitRepository.getAllHabits().filter { !it.isArchived }
            if (habits.isEmpty()) return Result.success()

            val today = LocalDate.now()
            val questsToGenerate = MAX_ACTIVE_QUESTS - activeQuests.size

            val generated = mutableListOf<Quest>()

            // Strategy 1: Streak builder — find habits with no streak
            val noStreakHabits = habits.filter {
                val streak = calculateStreakUseCase.currentStreak(it.id)
                streak == 0
            }
            if (noStreakHabits.isNotEmpty() && generated.size < questsToGenerate) {
                val habit = noStreakHabits.random()
                generated += Quest(
                    id = UUID.randomUUID().toString(),
                    title = "Kickstart: ${habit.name}",
                    description = "Complete ${habit.name} for 3 consecutive days to build momentum.",
                    difficulty = QuestDifficulty.DAILY,
                    xpReward = 50,
                    conditions = "streak:${habit.id}:3",
                    timeLimit = 3 * 24 * 60 * 60 * 1000L,
                    generatedBy = "QuestGeneratorWorker",
                    reasoning = "Habit has no active streak — nudging to build consistency.",
                    status = QuestStatus.AVAILABLE,
                    startedAt = null,
                    completedAt = null,
                )
            }

            // Strategy 2: Consistency challenge — complete all habits today
            val scheduledToday = habits.filter { it.isScheduledForDate(today) }
            if (scheduledToday.size >= 3 && generated.size < questsToGenerate) {
                generated += Quest(
                    id = UUID.randomUUID().toString(),
                    title = "Perfect Day",
                    description = "Complete all ${scheduledToday.size} habits scheduled for today.",
                    difficulty = QuestDifficulty.DAILY,
                    xpReward = 75,
                    conditions = "all_today",
                    timeLimit = 24 * 60 * 60 * 1000L,
                    generatedBy = "QuestGeneratorWorker",
                    reasoning = "Encouraging a full-completion day to boost confidence.",
                    status = QuestStatus.AVAILABLE,
                    startedAt = null,
                    completedAt = null,
                )
            }

            // Strategy 3: Weekly endurance — 7-day streak on a quantitative habit
            val quantHabits = habits.filter { it.trackingType == TrackingType.QUANTITATIVE }
            if (quantHabits.isNotEmpty() && generated.size < questsToGenerate) {
                val habit = quantHabits.random()
                generated += Quest(
                    id = UUID.randomUUID().toString(),
                    title = "Week Warrior: ${habit.name}",
                    description = "Hit your ${habit.name} target every day for a full week.",
                    difficulty = QuestDifficulty.WEEKLY,
                    xpReward = 150,
                    conditions = "streak:${habit.id}:7",
                    timeLimit = 7 * 24 * 60 * 60 * 1000L,
                    generatedBy = "QuestGeneratorWorker",
                    reasoning = "Weekly challenge for quantitative habit to push endurance.",
                    status = QuestStatus.AVAILABLE,
                    startedAt = null,
                    completedAt = null,
                )
            }

            generated.forEach { questRepository.insert(it) }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
