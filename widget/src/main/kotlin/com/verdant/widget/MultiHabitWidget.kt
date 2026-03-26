package com.verdant.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Multi-habit widget (4×2).
 *
 * Shows 2–5 user-selected habits with individual one-tap completion buttons.
 * Each row: icon | name | value/status | [toggle button]
 *
 * The set of habits is chosen via [WidgetConfigActivity] and stored as a
 * comma-separated string in [WidgetPreferencesKeys.MULTI_HABIT_IDS].
 * Toggle buttons reuse [ChecklistToggleReceiver] since they perform the same
 * binary-flip operation.
 */
class MultiHabitWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { MultiHabitContent() }
    }
}

class MultiHabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MultiHabitWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build(),
        )
    }
}
