package com.verdant.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.LocalDate

/**
 * Receives start/stop timer broadcasts from [TimerContent].
 *
 * Delegates to [WidgetUpdateWorker] which updates the Glance DataStore
 * timer keys and triggers a widget refresh. This keeps the receiver
 * simple and consistent with [ChecklistToggleReceiver].
 */
class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val action  = intent.action ?: return
        val date    = LocalDate.now().toString()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "timer_action_$habitId",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInputData(
                    workDataOf(
                        WidgetUpdateWorker.KEY_TIMER_ACTION    to action,
                        WidgetUpdateWorker.KEY_TIMER_LOG_HABIT_ID to habitId,
                        WidgetUpdateWorker.KEY_TIMER_LOG_DATE  to date,
                        WidgetUpdateWorker.KEY_TIMER_ACTION_TS to System.currentTimeMillis(),
                    )
                )
                .build()
        )
    }

    companion object {
        const val ACTION_START   = "com.verdant.widget.ACTION_TIMER_START"
        const val ACTION_STOP    = "com.verdant.widget.ACTION_TIMER_STOP"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}
