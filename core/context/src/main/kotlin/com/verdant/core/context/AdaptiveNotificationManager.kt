package com.verdant.core.context

import com.verdant.core.model.Habit
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationDecision(
    val shouldSend: Boolean,
    val reason: String? = null,
    val delayMinutes: Int = 0,
)

@Singleton
class AdaptiveNotificationManager @Inject constructor(
    private val contextSignalProvider: ContextSignalProvider,
    private val weatherContextProvider: WeatherContextProvider,
) {
    /**
     * Determines whether a notification should be sent for the given habit,
     * based on the current device context and weather conditions.
     */
    suspend fun shouldSendNotification(habit: Habit): NotificationDecision {
        val context = contextSignalProvider.getCurrentSignal()

        // Suppress outdoor habits during precipitation
        if (habit.outdoorActivity) {
            val weather = weatherContextProvider.getWeather()
            if (weather != null && weather.isPrecipitationLikely) {
                return NotificationDecision(
                    shouldSend = false,
                    reason = "Rain/snow expected — rescheduling outdoor activity",
                    delayMinutes = 180,
                )
            }
        }

        // Weekend mornings — reduce urgency for work-related habits
        if (context.isWeekend && context.currentHour < 10) {
            return NotificationDecision(shouldSend = true, delayMinutes = 60)
        }

        // Headphones connected — good time for music/podcast/meditation habits
        if (context.isHeadphonesConnected) {
            val musicRelated = listOf("music", "podcast", "meditat", "listen", "practice")
            if (musicRelated.any { habit.name.lowercase().contains(it) }) {
                return NotificationDecision(shouldSend = true)
            }
        }

        // Charging + evening — good time for productivity/learning
        if (context.isCharging && context.currentHour >= 19) {
            val productivityRelated = listOf("read", "study", "learn", "journal", "write")
            if (productivityRelated.any { habit.name.lowercase().contains(it) }) {
                return NotificationDecision(shouldSend = true)
            }
        }

        return NotificationDecision(shouldSend = true)
    }
}
