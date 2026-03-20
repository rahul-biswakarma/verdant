package com.verdant.core.common

import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

// ── Output data classes (all @Serializable for JSON transport to LLM) ────────

/**
 * Compact aggregated summary of a user's habit data, sized to stay under the
 * target LLM token budgets:
 *  - Daily insights  → target < 500 tokens   (~2 000 chars)
 *  - Weekly reports  → target < 800 tokens   (~3 200 chars)
 *  - Monthly reports → target < 1 000 tokens (~4 000 chars)
 */
@Serializable
data class AggregatedHabitData(
    val habits: List<HabitSummaryItem>,
    @SerialName("overallCompletionToday") val overallCompletionToday: Float,
    @SerialName("overallCompletionThisWeek") val overallCompletionThisWeek: Float,
    @SerialName("overallCompletionThisMonth") val overallCompletionThisMonth: Float,
    val topStreaks: List<StreakItem>,
    val periodDays: Int,
    /** Populated only for report payloads, null for daily insights */
    val weeklyBreakdown: List<DailyCompletion>? = null,
)

@Serializable
data class HabitSummaryItem(
    val id: String,
    val name: String,
    val icon: String,
    val trackingType: String,
    /** 0–1 completion rate over the aggregation period */
    val completionRate: Float,
    val currentStreak: Int,
    /** Mean value for QUANTITATIVE / DURATION / FINANCIAL habits; null for BINARY */
    val avgValue: Double? = null,
    val unit: String? = null,
)

@Serializable
data class StreakItem(
    val habitId: String,
    val habitName: String,
    val currentStreak: Int,
)

@Serializable
data class DailyCompletion(
    /** ISO date YYYY-MM-DD */
    val date: String,
    val completionRate: Float,
)

// ── Aggregator ────────────────────────────────────────────────────────────────

/**
 * Converts raw Room data into [AggregatedHabitData] compact enough to send
 * to an LLM without exceeding token budgets.
 *
 * All aggregation is performed in memory; no database calls are made here — the
 * caller is responsible for loading the required data before invoking this class.
 */
@Singleton
class HabitDataAggregator @Inject constructor() {

    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Produces a **daily insight** payload (< 500 token target).
     *
     * @param habits         All active habits.
     * @param entries        All entries for the aggregation period.
     * @param streaks        Current streak length per habit ID (0 = no active streak).
     * @param today          Reference date; used to compute today/week/month denominators.
     * @param periodDays     How many days of history to include in per-habit stats.
     */
    fun aggregateForDailyInsight(
        habits: List<Habit>,
        entries: List<HabitEntry>,
        streaks: Map<String, Int>,
        today: LocalDate = LocalDate.now(),
        periodDays: Int = 14,
    ): AggregatedHabitData = aggregate(
        habits = habits,
        entries = entries,
        streaks = streaks,
        today = today,
        periodDays = periodDays,
        includeWeeklyBreakdown = false,
    )

    /**
     * Produces a **report** payload (< 1 000 token target) with a daily breakdown.
     *
     * @param periodDays  Use 7 for weekly reports, 30 for monthly reports.
     */
    fun aggregateForReport(
        habits: List<Habit>,
        entries: List<HabitEntry>,
        streaks: Map<String, Int>,
        today: LocalDate = LocalDate.now(),
        periodDays: Int = 7,
    ): AggregatedHabitData = aggregate(
        habits = habits,
        entries = entries,
        streaks = streaks,
        today = today,
        periodDays = periodDays,
        includeWeeklyBreakdown = true,
    )

    // ── Core aggregation ──────────────────────────────────────────────────────

    private fun aggregate(
        habits: List<Habit>,
        entries: List<HabitEntry>,
        streaks: Map<String, Int>,
        today: LocalDate,
        periodDays: Int,
        includeWeeklyBreakdown: Boolean,
    ): AggregatedHabitData {
        val periodStart = today.minusDays(periodDays.toLong() - 1)

        // Index entries by habitId for O(1) lookups
        val entriesByHabit: Map<String, List<HabitEntry>> = entries
            .filter { it.date in periodStart..today }
            .groupBy { it.habitId }

        // ── Per-habit summaries ───────────────────────────────────────────
        val summaries = habits.map { habit ->
            val habitEntries = entriesByHabit[habit.id] ?: emptyList()
            val scheduled = scheduledDaysInPeriod(habit, periodStart, today)
            val completed = habitEntries.count { it.completed }
            val completionRate = if (scheduled > 0) completed.toFloat() / scheduled else 0f

            val avgValue: Double? = if (habit.trackingType != TrackingType.BINARY) {
                val values = habitEntries.mapNotNull { it.value }
                if (values.isNotEmpty()) values.average().roundTo(2) else null
            } else null

            HabitSummaryItem(
                id = habit.id,
                name = habit.name,
                icon = habit.icon,
                trackingType = habit.trackingType.name,
                completionRate = completionRate,
                currentStreak = streaks[habit.id] ?: 0,
                avgValue = avgValue,
                unit = habit.unit?.takeIf { it.isNotBlank() },
            )
        }

        // ── Overall completion rates ──────────────────────────────────────
        val overallToday = overallCompletionForDay(habits, entries, today)
        val overallWeek = overallCompletionForRange(
            habits, entries,
            start = today.with(DayOfWeek.MONDAY).coerceAtLeast(periodStart),
            end = today,
        )
        val overallMonth = overallCompletionForRange(
            habits, entries,
            start = today.withDayOfMonth(1).coerceAtLeast(periodStart),
            end = today,
        )

        // ── Top streaks (max 5, only active) ─────────────────────────────
        val topStreaks = habits
            .mapNotNull { h ->
                val streak = streaks[h.id] ?: 0
                if (streak > 0) StreakItem(h.id, h.name, streak) else null
            }
            .sortedByDescending { it.currentStreak }
            .take(5)

        // ── Daily breakdown (for reports) ─────────────────────────────────
        val weeklyBreakdown = if (includeWeeklyBreakdown) {
            (0 until periodDays).map { offset ->
                val date = periodStart.plusDays(offset.toLong())
                DailyCompletion(
                    date = date.format(isoDate),
                    completionRate = overallCompletionForDay(habits, entries, date),
                )
            }
        } else null

        return AggregatedHabitData(
            habits = summaries,
            overallCompletionToday = overallToday,
            overallCompletionThisWeek = overallWeek,
            overallCompletionThisMonth = overallMonth,
            topStreaks = topStreaks,
            periodDays = periodDays,
            weeklyBreakdown = weeklyBreakdown,
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the number of days in the range start..end that [habit] is scheduled for. */
    private fun scheduledDaysInPeriod(habit: Habit, start: LocalDate, end: LocalDate): Int {
        var count = 0
        var d = start
        while (!d.isAfter(end)) {
            if (habit.isScheduledOn(d)) count++
            d = d.plusDays(1)
        }
        return count
    }

    private fun Habit.isScheduledOn(date: LocalDate): Boolean = when (frequency) {
        HabitFrequency.DAILY -> true
        HabitFrequency.WEEKDAYS -> date.dayOfWeek.value in 1..5
        HabitFrequency.WEEKENDS -> date.dayOfWeek.value in 6..7
        HabitFrequency.SPECIFIC_DAYS -> {
            val bit = 1 shl (date.dayOfWeek.value - 1)
            scheduleDays and bit != 0
        }
        HabitFrequency.TIMES_PER_WEEK -> true // treat as scheduled every day for rate calc
    }

    private fun overallCompletionForDay(
        habits: List<Habit>,
        entries: List<HabitEntry>,
        date: LocalDate,
    ): Float {
        val scheduled = habits.filter { it.isScheduledOn(date) }
        if (scheduled.isEmpty()) return 0f
        val completedIds = entries.filter { it.date == date && it.completed }.map { it.habitId }.toSet()
        return completedIds.intersect(scheduled.map { it.id }.toSet()).size.toFloat() / scheduled.size
    }

    private fun overallCompletionForRange(
        habits: List<Habit>,
        entries: List<HabitEntry>,
        start: LocalDate,
        end: LocalDate,
    ): Float {
        var totalScheduled = 0
        var totalCompleted = 0
        val completedSet = entries.filter { it.completed }.map { "${it.habitId}:${it.date}" }.toSet()

        var d = start
        while (!d.isAfter(end)) {
            habits.filter { it.isScheduledOn(d) }.forEach { habit ->
                totalScheduled++
                if ("${habit.id}:$d" in completedSet) totalCompleted++
            }
            d = d.plusDays(1)
        }

        return if (totalScheduled > 0) totalCompleted.toFloat() / totalScheduled else 0f
    }

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}
