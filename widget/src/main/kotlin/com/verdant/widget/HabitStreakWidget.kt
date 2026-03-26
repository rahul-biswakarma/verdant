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
 * 2×2 per-habit streak widget.
 *
 * Shows the current streak count for a single selected habit along with a
 * mini circular progress indicator reflecting the 30-day completion rate.
 *
 * Compare with [StreakWidget] which shows the top 3 streaks across all habits.
 */
class HabitStreakWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { HabitStreakContent() }
    }
}

class HabitStreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = HabitStreakWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build(),
        )
    }
}
