package com.verdant.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.LocalDate

/**
 * Receives toggle-completion broadcasts from [ChecklistContent] rows.
 * Enqueues [WidgetUpdateWorker] with the habitId+date so the toggle
 * and full state refresh happen atomically in the worker.
 */
class ChecklistToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val date    = intent.getStringExtra(EXTRA_DATE) ?: LocalDate.now().toString()

        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInputData(workDataOf(
                    WidgetUpdateWorker.KEY_TOGGLE_HABIT_ID to habitId,
                    WidgetUpdateWorker.KEY_TOGGLE_DATE     to date,
                ))
                .build()
        )
    }

    companion object {
        const val ACTION_TOGGLE  = "com.verdant.widget.ACTION_TOGGLE_CHECKLIST"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_DATE     = "date"
    }
}
