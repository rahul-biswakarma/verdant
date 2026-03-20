package com.verdant.work.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.verdant.core.database.usecase.LogEntryUseCase
import com.verdant.work.notification.NotificationChannels
import com.verdant.work.scheduler.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Handles inline notification actions for both streak-nudge and reminder notifications.
 *
 * Actions:
 *  - [ACTION_DONE]   → log binary completion for today, dismiss notification.
 *  - [ACTION_SKIP]   → log skip for today, dismiss notification.
 *  - [ACTION_SNOOZE] → dismiss notification, re-fire in 1 hour via AlarmManager.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var logEntryUseCase: LogEntryUseCase
    @Inject lateinit var reminderScheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return

        when (intent.action) {
            ACTION_DONE   -> handleDone(context, habitId)
            ACTION_SKIP   -> handleSkip(context, habitId)
            ACTION_SNOOZE -> handleSnooze(context, habitId, intent)
        }
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private fun handleDone(context: Context, habitId: String) {
        dismissNotification(context, habitId)
        scope.launch {
            runCatching {
                logEntryUseCase.logBinary(habitId, LocalDate.now(), completed = true)
            }
        }
    }

    private fun handleSkip(context: Context, habitId: String) {
        dismissNotification(context, habitId)
        scope.launch {
            runCatching {
                logEntryUseCase.skip(habitId, LocalDate.now())
            }
        }
    }

    private fun handleSnooze(context: Context, habitId: String, original: Intent) {
        dismissNotification(context, habitId)

        // Schedule a re-fire in 1 hour
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerMs = ZonedDateTime.now(ZoneId.systemDefault())
            .plusHours(SNOOZE_HOURS)
            .toInstant()
            .toEpochMilli()

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_FIRE
            putExtra(EXTRA_HABIT_ID,   habitId)
            putExtra(EXTRA_HABIT_NAME, original.getStringExtra(EXTRA_HABIT_NAME) ?: "")
        }
        val pending = PendingIntent.getBroadcast(
            context,
            "snooze_$habitId".hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pending)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun dismissNotification(context: Context, habitId: String) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NotificationChannels.streakNudgeId(habitId))
            cancel(NotificationChannels.reminderId(habitId))
        }
    }

    companion object {
        const val ACTION_DONE        = "com.verdant.work.ACTION_DONE"
        const val ACTION_SKIP        = "com.verdant.work.ACTION_SKIP"
        const val ACTION_SNOOZE      = "com.verdant.work.ACTION_SNOOZE"
        /** Internal re-fire action after snooze delay. */
        const val ACTION_SNOOZE_FIRE = "com.verdant.work.ACTION_SNOOZE_FIRE"

        const val EXTRA_HABIT_ID   = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"

        private const val SNOOZE_HOURS = 1L
    }
}
