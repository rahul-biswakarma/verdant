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
 * Timer widget for DURATION habits (2×2).
 *
 * Lets users start and stop a session timer directly from the home screen.
 * Elapsed time is shown in HH:MM:SS format with a circular progress arc.
 * On stop, logs the elapsed duration via [LogEntryUseCase.setQuantitative].
 *
 * Requires a habit to be selected during config ([WidgetConfigActivity]).
 */
class TimerWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { TimerContent() }
    }
}

class TimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TimerWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build(),
        )
    }
}
