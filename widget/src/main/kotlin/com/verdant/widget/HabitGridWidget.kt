package com.verdant.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

class HabitGridWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    /**
     * Three responsive breakpoints map to widget sizes:
     *  110×110 dp  →  2×2 cell  →  4 weeks
     *  220×110 dp  →  4×2 cell  →  12 weeks
     *  220×176 dp  →  4×3 cell  →  20 weeks
     */
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(220.dp, 110.dp),
            DpSize(220.dp, 176.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            HabitGridContent()
        }
    }
}

class HabitGridWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget = HabitGridWidget()

    /** Schedule periodic refresh when the first widget instance is added. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build(),
        )
    }

    /** Cancel periodic refresh when the last widget instance is removed. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(WidgetUpdateWorker.PERIODIC_WORK_NAME)
    }
}
