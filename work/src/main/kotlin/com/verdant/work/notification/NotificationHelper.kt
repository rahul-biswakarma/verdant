package com.verdant.work.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.verdant.work.R
import com.verdant.work.receiver.NotificationActionReceiver

/**
 * Builds and posts [Notification]s for every Verdant notification type.
 *
 * All public methods are idempotent and safe to call on a background thread.
 */
object NotificationHelper {

    // ── Public builders ───────────────────────────────────────────────────────

    fun postDailyMotivation(context: Context, message: String) {
        val notification = base(context, NotificationChannels.CHANNEL_MOTIVATION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Your daily motivation ✨")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        notify(context, NotificationChannels.NOTIF_DAILY_MOTIVATION, notification)
    }

    fun postStreakNudge(
        context: Context,
        habitId: String,
        habitName: String,
        streakDays: Int,
        nudgeText: String,
    ) {
        val doneIntent   = actionIntent(context, habitId, NotificationActionReceiver.ACTION_DONE)
        val skipIntent   = actionIntent(context, habitId, NotificationActionReceiver.ACTION_SKIP)
        val snoozeIntent = actionIntent(context, habitId, NotificationActionReceiver.ACTION_SNOOZE)

        val notification = base(context, NotificationChannels.CHANNEL_STREAKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔥 $streakDays-day streak at risk — $habitName")
            .setContentText(nudgeText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(nudgeText))
            .addAction(0, "Done",   doneIntent)
            .addAction(0, "Skip",   skipIntent)
            .addAction(0, "Snooze", snoozeIntent)
            .setAutoCancel(true)
            .build()
        notify(context, NotificationChannels.streakNudgeId(habitId), notification)
    }

    fun postHabitReminder(
        context: Context,
        habitId: String,
        habitName: String,
        reminderNote: String?,
    ) {
        val doneIntent   = actionIntent(context, habitId, NotificationActionReceiver.ACTION_DONE)
        val snoozeIntent = actionIntent(context, habitId, NotificationActionReceiver.ACTION_SNOOZE)

        val body = reminderNote?.takeIf { it.isNotBlank() }
            ?: "Tap to log your progress for today."

        val notification = base(context, NotificationChannels.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for: $habitName")
            .setContentText(body)
            .addAction(0, "Done",   doneIntent)
            .addAction(0, "Snooze", snoozeIntent)
            .setAutoCancel(true)
            .build()
        notify(context, NotificationChannels.reminderId(habitId), notification)
    }

    fun postWeeklySummary(context: Context, summary: String) {
        val notification = base(context, NotificationChannels.CHANNEL_INSIGHTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Your week in review 📊")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setAutoCancel(true)
            .build()
        notify(context, NotificationChannels.NOTIF_WEEKLY_SUMMARY, notification)
    }

    fun postMilestone(context: Context, habitId: String, habitName: String, streakDays: Int) {
        val emoji = when {
            streakDays >= 365 -> "🏆"
            streakDays >= 100 -> "💎"
            streakDays >= 30  -> "🌟"
            else              -> "🎉"
        }
        val title = "$emoji $streakDays-day streak — $habitName!"
        val body  = "You've kept up $habitName for $streakDays days in a row. Keep going!"

        val notification = base(context, NotificationChannels.CHANNEL_MILESTONES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        notify(context, NotificationChannels.milestoneId(habitId), notification)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun base(context: Context, channelId: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    private fun actionIntent(context: Context, habitId: String, action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_HABIT_ID, habitId)
        }
        val reqCode = "${action}_${habitId}".hashCode()
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
        // Silently swallow SecurityException on API 33+ if POST_NOTIFICATIONS was denied.
    }
}
