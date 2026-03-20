package com.verdant.work.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Centralised definitions for every notification channel Verdant uses.
 * Call [createAll] once from [com.verdant.app.VerdantApplication.onCreate].
 */
object NotificationChannels {

    // ── Channel IDs ───────────────────────────────────────────────────────────

    /** Per-habit reminders set by the user. High priority, sound. */
    const val CHANNEL_REMINDERS = "verdant_reminders"

    /** Streak-at-risk nudges (StreakAlertWorker). Default priority. */
    const val CHANNEL_STREAKS = "verdant_streaks"

    /** Daily morning motivation (DailyMotivationWorker). Low priority. */
    const val CHANNEL_MOTIVATION = "verdant_motivation"

    /** AI-generated insights shown in the feed. Low priority. */
    const val CHANNEL_INSIGHTS = "verdant_insights"

    /** Milestone achievements (7-day, 30-day, …). Default priority. */
    const val CHANNEL_MILESTONES = "verdant_milestones"

    // ── Notification IDs ──────────────────────────────────────────────────────
    // Deterministic IDs avoid duplicate notifications; reminder IDs are derived
    // from the habit's stable hash so each habit gets its own slot.

    const val NOTIF_DAILY_MOTIVATION = 10_001
    const val NOTIF_WEEKLY_SUMMARY   = 10_002

    /** Streak-nudge notifications use IDs in [20_000, 20_999]. */
    fun streakNudgeId(habitId: String): Int = 20_000 + (habitId.hashCode() and 0x3FF)

    /** Per-habit reminder IDs live in [30_000, 30_999]. */
    fun reminderId(habitId: String): Int = 30_000 + (habitId.hashCode() and 0x3FF)

    /** Milestone notification IDs live in [40_000, 40_999]. */
    fun milestoneId(habitId: String): Int = 40_000 + (habitId.hashCode() and 0x3FF)

    // ── Channel creation ──────────────────────────────────────────────────────

    fun createAll(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Habit Reminders",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Reminders to complete your scheduled habits"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_STREAKS,
                    "Streak Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Nudges when your streaks are at risk"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_MOTIVATION,
                    "Daily Motivation",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Your personalised morning motivation message"
                },
                NotificationChannel(
                    CHANNEL_INSIGHTS,
                    "AI Insights",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Habit patterns and suggestions from your AI coach"
                },
                NotificationChannel(
                    CHANNEL_MILESTONES,
                    "Milestones",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Celebrations when you hit streak milestones"
                    enableVibration(true)
                },
            )
        )
    }
}
