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
 * 4×2 configurable widget showing 3–5 selected habits with per-habit toggle buttons.
 *
 * The user picks which habits appear via [WidgetConfigActivity].
 * Habit IDs are stored as a comma-separated string in [WidgetPreferencesKeys.MULTI_HABIT_IDS].
 * Each row includes a toggle button that broadcasts [ChecklistToggleReceiver.ACTION_TOGGLE].
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
