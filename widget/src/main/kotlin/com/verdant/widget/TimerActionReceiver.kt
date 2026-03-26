package com.verdant.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.LocalDate

/**
 * Handles timer start / stop broadcasts from [TimerContent].
 *
 * - [ACTION_TIMER_START]: enqueues worker with KEY_TIMER_START_HABIT_ID so the
 *   worker can mark the widget running and record the start epoch.
 * - [ACTION_TIMER_STOP]: enqueues worker with KEY_TIMER_STOP_HABIT_ID +
 *   KEY_TIMER_ELAPSED (computed by the content composable at tap-time) +
 *   KEY_TIMER_INTENSITY; the worker logs the duration entry and resets state.
 * - [ACTION_TIMER_INTENSITY]: cycle the intensity level 1→2→3→4→5→1.
 */
class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return

        val inputData = when (intent.action) {
            ACTION_TIMER_START -> workDataOf(
                WidgetUpdateWorker.KEY_TIMER_START_HABIT_ID to habitId,
            )
            ACTION_TIMER_STOP -> workDataOf(
                WidgetUpdateWorker.KEY_TIMER_STOP_HABIT_ID to habitId,
                WidgetUpdateWorker.KEY_TIMER_ELAPSED       to intent.getLongExtra(EXTRA_ELAPSED, 0L),
                WidgetUpdateWorker.KEY_TIMER_INTENSITY     to intent.getIntExtra(EXTRA_INTENSITY, 3),
                WidgetUpdateWorker.KEY_TIMER_DATE          to (intent.getStringExtra(EXTRA_DATE) ?: LocalDate.now().toString()),
            )
            ACTION_TIMER_INTENSITY -> workDataOf(
                WidgetUpdateWorker.KEY_TIMER_INTENSITY_HABIT_ID to habitId,
                WidgetUpdateWorker.KEY_TIMER_INTENSITY          to intent.getIntExtra(EXTRA_INTENSITY, 3),
            )
            else -> return
        }

        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInputData(inputData)
                .build()
        )
    }

    companion object {
        const val ACTION_TIMER_START     = "com.verdant.widget.ACTION_TIMER_START"
        const val ACTION_TIMER_STOP      = "com.verdant.widget.ACTION_TIMER_STOP"
        const val ACTION_TIMER_INTENSITY = "com.verdant.widget.ACTION_TIMER_INTENSITY"

        const val EXTRA_HABIT_ID  = "habit_id"
        const val EXTRA_ELAPSED   = "elapsed_secs"
        const val EXTRA_INTENSITY = "intensity"
        const val EXTRA_DATE      = "date"
    }
}
