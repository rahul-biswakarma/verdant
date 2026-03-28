package com.verdant.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.model.repository.HabitRepository
import com.verdant.work.notification.NotificationHelper
import com.verdant.work.scheduler.ReminderScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-shot worker that fires a single habit reminder notification and then
 * re-schedules the next alarm via [ReminderScheduler].
 *
 * This worker is enqueued by [ReminderScheduler] as a fallback path when
 * the [com.verdant.work.receiver.ReminderAlarmReceiver] cannot be used
 * (e.g. exact alarm permission revoked on API 31+). In the normal flow,
 * [ReminderAlarmReceiver] handles reminder delivery directly.
 *
 * Input data must contain [KEY_HABIT_ID].
 */
@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val scheduler: ReminderScheduler,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(KEY_HABIT_ID) ?: return Result.failure()

        val habit = habitRepository.getById(habitId) ?: return Result.failure()

        // Post the reminder notification
        NotificationHelper.postHabitReminder(
            context = applicationContext,
            habitId = habit.id,
            habitName = habit.name,
            reminderNote = null,
        )

        // Re-schedule the next occurrence so reminders recur automatically
        if (habit.reminderEnabled) {
            scheduler.scheduleReminder(habit)
        }

        return Result.success()
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
    }
}
