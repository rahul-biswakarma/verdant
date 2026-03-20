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
 * 4×2 widget: left side = 8-week contribution grid, right side = streak stats.
 * Requires a specific habit to be configured (like [HabitGridWidget]).
 */
class MiniHeatmapWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { MiniHeatmapContent() }
    }
}

class MiniHeatmapWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MiniHeatmapWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build(),
        )
    }
}
