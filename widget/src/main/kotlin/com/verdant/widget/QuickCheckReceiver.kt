package com.verdant.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * Handles the quick-check tap on binary habit widget cells.
 *
 * Rather than injecting [LogEntryUseCase] directly (which requires goAsync()
 * and a coroutine scope), we delegate to [WidgetUpdateWorker] with the habit
 * and date in the input data. The worker logs the entry atomically then
 * refreshes all widget states — no race condition.
 *
 * Registered in the manifest with `android:exported="false"`.
 */
class QuickCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_QUICK_CHECK) return

        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val date    = intent.getStringExtra(EXTRA_DATE)     ?: java.time.LocalDate.now().toString()

        val work = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setInputData(
                workDataOf(
                    WidgetUpdateWorker.KEY_QUICK_LOG_HABIT_ID to habitId,
                    WidgetUpdateWorker.KEY_QUICK_LOG_DATE      to date,
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }

    companion object {
        const val ACTION_QUICK_CHECK = "com.verdant.widget.ACTION_QUICK_CHECK"
        const val EXTRA_HABIT_ID     = "habit_id"
        const val EXTRA_DATE         = "date"
    }
}
