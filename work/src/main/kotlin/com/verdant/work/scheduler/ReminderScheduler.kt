package com.verdant.work.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.verdant.core.model.Habit
import com.verdant.work.receiver.ReminderAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels [AlarmManager] exact alarms for per-habit reminders.
 *
 * ## Alarm lifecycle
 * - [scheduleReminder] fires one exact alarm at the next occurrence of the
 *   habit's [Habit.reminderTime] on a scheduled day.
 * - [ReminderAlarmReceiver] receives the alarm, posts a notification, then
 *   calls [scheduleReminder] again to set the *following* occurrence.
 * - [cancelReminder] cancels the alarm when the user turns off reminders for
 *   a habit or deletes it.
 * - [rescheduleAll] is called on boot (via [com.verdant.work.receiver.BootReceiver])
 *   to restore all active reminders after a device reboot.
 *
 * ## Permissions
 * Requires `SCHEDULE_EXACT_ALARM` (or `USE_EXACT_ALARM`) on Android 12+.
 * The Manifest declares `USE_EXACT_ALARM` so no runtime prompt is needed for
 * habit-reminder style alarms.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Schedules the next alarm(s) for [habit].
     *
     * Supports comma-separated reminder times (e.g., "07:00,19:00") for
     * multiple daily reminders. Each time gets its own alarm with a unique
     * PendingIntent keyed on `"${habitId}_${index}"`.
     *
     * No-ops if reminders are disabled on the habit or the habit has no
     * [Habit.reminderTime].
     */
    fun scheduleReminder(habit: Habit) {
        if (!habit.reminderEnabled) return
        val rawTimes = habit.reminderTime ?: return
        val times = rawTimes.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (times.isEmpty()) return

        times.forEachIndexed { index, timeStr ->
            val time = runCatching { LocalTime.parse(timeStr, timeFormatter) }.getOrNull() ?: return@forEachIndexed
            val nextTrigger = nextOccurrence(time, habit.reminderDays) ?: return@forEachIndexed
            val triggerMs = nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pending = buildPendingIntent(habit, index)
            setExactAlarm(triggerMs, pending)
        }
    }

    /**
     * Cancels all alarms for [habit]. Safe to call even if no alarm is set.
     * Cancels up to [MAX_REMINDER_SLOTS] alarms to cover any number of
     * comma-separated reminder times.
     */
    fun cancelReminder(habit: Habit) {
        for (index in 0 until MAX_REMINDER_SLOTS) {
            val pending = buildPendingIntent(habit, index)
            alarmManager.cancel(pending)
        }
    }

    /**
     * Cancels and re-schedules reminders for all [habits] that have reminders
     * enabled. Called from [com.verdant.work.receiver.BootReceiver].
     */
    fun rescheduleAll(habits: List<Habit>) {
        for (habit in habits) {
            cancelReminder(habit)
            if (habit.reminderEnabled) scheduleReminder(habit)
        }
    }


    /**
     * Returns the next [LocalDateTime] on or after now that satisfies [time]
     * and falls on a day set in [reminderDaysMask] (bitmask Mon=1…Sun=64).
     * Returns null if the mask has no days set.
     */
    private fun nextOccurrence(time: LocalTime, reminderDaysMask: Int): LocalDateTime? {
        if (reminderDaysMask == 0) return null
        val now    = LocalDateTime.now()
        var candidate = LocalDate.now().atTime(time)

        // If the alarm time already passed today, start from tomorrow
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)

        // Walk forward up to 7 days to find a scheduled day
        repeat(7) { offset ->
            val dayBit = 1 shl (candidate.dayOfWeek.value - 1) // Mon=0→bit1
            if (reminderDaysMask and dayBit != 0) return candidate
            candidate = candidate.plusDays(1)
        }
        return null
    }

    private fun buildPendingIntent(habit: Habit, timeIndex: Int = 0): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_FIRE_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_HABIT_ID,   habit.id)
            putExtra(ReminderAlarmReceiver.EXTRA_HABIT_NAME, habit.name)
        }
        val requestCode = "${habit.id}_$timeIndex".hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Maximum number of reminder time slots per habit. */
        private const val MAX_REMINDER_SLOTS = 6
    }

    private fun setExactAlarm(triggerMs: Long, pending: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            // Exact-alarm permission not granted — fall back to inexact
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pending)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pending)
    }
}
