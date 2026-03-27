package com.verdant.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Timer widget UI (2×2).
 *
 * Displays elapsed/accumulated session time and a start/stop button.
 * The [TimerActionReceiver] handles state transitions.
 */
@Composable
internal fun TimerContent() {
    val prefs     = currentState<Preferences>()
    val context   = LocalContext.current

    val habitId    = prefs[WidgetPreferencesKeys.HABIT_ID]          ?: ""
    val habitName  = prefs[WidgetPreferencesKeys.HABIT_NAME]         ?: "Habit"
    val habitIcon  = prefs[WidgetPreferencesKeys.HABIT_ICON]         ?: "🌱"
    val colorLong  = prefs[WidgetPreferencesKeys.HABIT_COLOR]        ?: 0xFF4CAF50L
    val startMs    = prefs[WidgetPreferencesKeys.TIMER_START_MS]     ?: 0L
    val totalSecs  = prefs[WidgetPreferencesKeys.TIMER_TOTAL_SECONDS] ?: 0

    val isRunning   = startMs > 0L
    val habitColor  = Color(colorLong.toInt())

    // Calculate elapsed seconds if running
    val elapsedSecs = if (isRunning) {
        ((System.currentTimeMillis() - startMs) / 1000L).toInt() + totalSecs
    } else {
        totalSecs
    }

    val timerText = formatSeconds(elapsedSecs)
    val action    = if (isRunning) TimerActionReceiver.ACTION_STOP else TimerActionReceiver.ACTION_START

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .cornerRadius(16.dp)
            .padding(12.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = habitIcon,
                    style = TextStyle(fontSize = 14.sp),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = habitName,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE0E0E0)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            // Timer display
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = timerText,
                    style = TextStyle(
                        color = ColorProvider(if (isRunning) habitColor else Color(0xFFE0E0E0)),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            // Start / Stop button
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(if (isRunning) Color(0xFFE53935) else habitColor)
                    .cornerRadius(18.dp)
                    .clickable(
                        actionSendBroadcast(
                            Intent(action).apply {
                                setPackage(context.packageName)
                                putExtra(TimerActionReceiver.EXTRA_HABIT_ID, habitId)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isRunning) "⏹ Stop" else "▶ Start",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

private fun formatSeconds(totalSecs: Int): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}
