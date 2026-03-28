package com.verdant.core.database.usecase

import com.verdant.core.database.dao.HabitTargetHistoryDao
import com.verdant.core.database.entity.HabitTargetHistoryEntity
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.model.Habit
import com.verdant.core.model.TrackingType
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class TargetSuggestion(
    val habitId: String,
    val habitName: String,
    val currentTarget: Double,
    val suggestedTarget: Double,
    val direction: Direction,
    val reason: String,
) {
    enum class Direction { INCREASE, DECREASE }
}

class AdaptiveTargetUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val targetHistoryDao: HabitTargetHistoryDao,
) {
    /**
     * Scans all QUANTITATIVE/DURATION habits and returns target suggestions
     * based on 7-day rolling performance.
     */
    suspend fun evaluateAll(): List<TargetSuggestion> {
        val habits = habitRepository.getAllHabits()
            .filter { !it.isArchived && (it.targetValue ?: 0.0) > 0 }
            .filter { it.trackingType == TrackingType.QUANTITATIVE || it.trackingType == TrackingType.DURATION }

        return habits.mapNotNull { evaluate(it) }
    }

    suspend fun evaluate(habit: Habit): TargetSuggestion? {
        val target = habit.targetValue ?: return null
        if (target <= 0) return null

        val today = LocalDate.now()
        val last7Days = (0 until 7).map { today.minusDays(it.toLong()) }

        val entries = last7Days.mapNotNull { date ->
            entryRepository.getByHabitAndDate(habit.id, date)
        }

        if (entries.size < 4) return null // Need at least 4 days of data

        val completedDays = entries.count { it.completed }
        val completionRate = completedDays.toDouble() / 7.0
        val values = entries.mapNotNull { it.value }
        val rollingAvg = if (values.isNotEmpty()) values.average() else return null

        return when {
            completionRate > 0.90 && rollingAvg > target * 1.15 -> {
                val newTarget = target * 1.10
                TargetSuggestion(
                    habitId = habit.id,
                    habitName = habit.name,
                    currentTarget = target,
                    suggestedTarget = newTarget,
                    direction = TargetSuggestion.Direction.INCREASE,
                    reason = "You've averaged ${String.format("%.1f", rollingAvg)} this week — 15%+ above your target. Ready to level up?",
                )
            }
            completionRate < 0.40 -> {
                val newTarget = target * 0.90
                TargetSuggestion(
                    habitId = habit.id,
                    habitName = habit.name,
                    currentTarget = target,
                    suggestedTarget = newTarget,
                    direction = TargetSuggestion.Direction.DECREASE,
                    reason = "Completion is at ${(completionRate * 100).toInt()}%. A slightly lower target may help build consistency.",
                )
            }
            else -> null
        }
    }

    suspend fun applyTargetChange(habitId: String, oldTarget: Double, newTarget: Double) {
        habitRepository.updateTarget(habitId, newTarget)
        targetHistoryDao.insert(
            HabitTargetHistoryEntity(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                oldTarget = oldTarget,
                newTarget = newTarget,
                changedAt = System.currentTimeMillis(),
                reason = "AUTO_SUGGESTED",
            ),
        )
    }
}
