package com.verdant.work.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.entity.toDomain
import com.verdant.work.notification.NotificationHelper
import com.verdant.work.scheduler.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fired by [ReminderScheduler] when a habit's alarm triggers.
 *
 * Responsibilities:
 *  1. Post a [NotificationHelper.postHabitReminder] notification.
 *  2. Load the habit from Room and re-schedule the *next* occurrence via
 *     [ReminderScheduler.scheduleReminder] so reminders recur automatically.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var habitDao: HabitDao
    @Inject lateinit var scheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_REMINDER) return

        val habitId   = intent.getStringExtra(EXTRA_HABIT_ID)   ?: return
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: return

        // Post the notification immediately (no DB needed for the title)
        NotificationHelper.postHabitReminder(
            context     = context,
            habitId     = habitId,
            habitName   = habitName,
            reminderNote = null,
        )

        // Re-schedule next occurrence on a background coroutine
        scope.launch {
            val entity = habitDao.getById(habitId) ?: return@launch
            val habit  = entity.toDomain()
            if (habit.reminderEnabled) {
                scheduler.scheduleReminder(habit)
            }
        }
    }

    companion object {
        const val ACTION_FIRE_REMINDER = "com.verdant.work.ACTION_FIRE_REMINDER"
        const val EXTRA_HABIT_ID       = "habit_id"
        const val EXTRA_HABIT_NAME     = "habit_name"
    }
}
