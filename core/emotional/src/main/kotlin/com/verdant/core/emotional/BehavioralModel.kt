package com.verdant.core.emotional

class BehavioralModel {

    data class DayOfWeekPattern(
        val dayOfWeek: Int,
        val completionRate: Float,
        val averageCompletionHour: Int,
    )

    data class TriggerChain(
        val triggerHabitId: String,
        val affectedHabitId: String,
        val correlation: Float,
    )

    fun findDayOfWeekPatterns(
        completionsByDay: Map<Int, List<Boolean>>,
    ): List<DayOfWeekPattern> {
        return completionsByDay.map { (day, completions) ->
            DayOfWeekPattern(
                dayOfWeek = day,
                completionRate = if (completions.isEmpty()) 0f
                    else completions.count { it }.toFloat() / completions.size,
                averageCompletionHour = 12,
            )
        }
    }

    fun findTriggerChains(
        habitCompletions: Map<String, List<Boolean>>,
    ): List<TriggerChain> {
        val chains = mutableListOf<TriggerChain>()
        val habitIds = habitCompletions.keys.toList()

        for (i in habitIds.indices) {
            for (j in i + 1 until habitIds.size) {
                val a = habitCompletions[habitIds[i]] ?: continue
                val b = habitCompletions[habitIds[j]] ?: continue
                val minLen = minOf(a.size, b.size)
                if (minLen < 7) continue

                val coOccurrence = (0 until minLen).count { a[it] && b[it] }.toFloat() / minLen
                if (coOccurrence > 0.6f) {
                    chains.add(TriggerChain(habitIds[i], habitIds[j], coOccurrence))
                }
            }
        }
        return chains
    }
}
