package com.verdant.core.database.usecase

import com.verdant.core.database.repository.HabitEntryRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Computes streak metrics for a single habit.
 *
 * "Current streak" counts consecutive completed days ending on today (or
 * yesterday if today has no entry yet). "Longest streak" is the maximum
 * run of completed consecutive days over all time.
 */
class CalculateStreakUseCase @Inject constructor(
    private val entryRepository: HabitEntryRepository,
) {

    /** Returns the current active streak length (0 if none). */
    suspend fun currentStreak(habitId: String): Int {
        val completedSet = entryRepository.getCompletedDates(habitId).toHashSet()
        val today = LocalDate.now()

        // Allow streak to continue if today hasn't been logged yet
        val startDate = if (today in completedSet) today else today.minusDays(1)
        if (startDate !in completedSet) return 0

        var streak = 0
        var date = startDate
        while (date in completedSet) {
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    /** Returns the all-time longest streak length. */
    suspend fun longestStreak(habitId: String): Int {
        val sorted = entryRepository.getCompletedDates(habitId)
            .sorted()   // ascending
        if (sorted.isEmpty()) return 0

        var maxStreak = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                current++
                if (current > maxStreak) maxStreak = current
            } else {
                current = 1
            }
        }
        return maxStreak
    }

    /** Returns a map of habitId → current streak for a list of habits. */
    suspend fun currentStreaks(habitIds: List<String>): Map<String, Int> =
        habitIds.associateWith { currentStreak(it) }

    /** Completion rate for the last [days] days (0..1). */
    suspend fun completionRate(habitId: String, days: Int = 30): Float {
        val today = LocalDate.now()
        val start = today.minusDays(days.toLong() - 1)
        val completedSet = entryRepository.getCompletedDates(habitId).toHashSet()
        val completedInWindow = (0 until days).count { offset ->
            start.plusDays(offset.toLong()) in completedSet
        }
        return completedInWindow.toFloat() / days
    }
}
