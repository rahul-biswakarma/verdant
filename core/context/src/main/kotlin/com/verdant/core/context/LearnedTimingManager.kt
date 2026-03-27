package com.verdant.core.context

import com.verdant.core.datastore.UserPreferencesDataStore
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LearnedTimingManager @Inject constructor(
    @Suppress("unused")
    private val prefs: UserPreferencesDataStore,
) {
    // In-memory tracking: habitId -> list of (notificationHour, completedWithin1h: Boolean)
    private val completionLog = mutableMapOf<String, MutableList<Pair<Int, Boolean>>>()

    fun recordNotificationSent(habitId: String, hour: Int) {
        completionLog.getOrPut(habitId) { mutableListOf() }
            .add(hour to false)
    }

    fun recordCompletion(habitId: String) {
        val log = completionLog[habitId] ?: return
        val currentHour = LocalTime.now().hour
        // Mark the most recent notification within 1 hour as successful
        log.indices.reversed().firstOrNull { i ->
            !log[i].second && abs(log[i].first - currentHour) <= 1
        }?.let { i ->
            log[i] = log[i].first to true
        }
    }

    /**
     * After 2 weeks of data (14+ entries), returns the optimal hour for a habit.
     * Returns null if insufficient data.
     */
    fun getOptimalHour(habitId: String): Int? {
        val log = completionLog[habitId] ?: return null
        if (log.size < 14) return null

        // Find the hour with the highest success rate
        val hourStats = log.groupBy { it.first }
            .mapValues { (_, entries) ->
                val total = entries.size.toFloat()
                val successes = entries.count { it.second }
                if (total >= 3) successes / total else 0f
            }

        return hourStats.maxByOrNull { it.value }
            ?.takeIf { it.value > 0.5f }
            ?.key
    }
}
