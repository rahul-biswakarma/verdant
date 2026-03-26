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
import java.time.LocalDate

/**
 * Timer widget composable (2×2) for DURATION habits.
 *
 * Layout:
 *  - Header: habit icon + name
 *  - Center: HH:MM:SS elapsed counter + progress fraction
 *  - Bottom: Start/Stop button  |  intensity dots (1–5)
 *
 * Elapsed time displayed at tap-time:
 *  - Running: currentEpoch − startEpoch + previousElapsed
 *  - Stopped: previousElapsed
 *
 * The content composable passes the computed elapsed to [TimerActionReceiver]
 * so the worker can log the exact duration without a separate clock lookup.
 */
@Composable
internal fun TimerContent() {
    val prefs   = currentState<Preferences>()
    val context = LocalContext.current
    val today   = LocalDate.now().toString()

    val habitId     = prefs[WidgetPreferencesKeys.HABIT_ID]         ?: ""
    val habitName   = prefs[WidgetPreferencesKeys.HABIT_NAME]       ?: "Duration"
    val habitIcon   = prefs[WidgetPreferencesKeys.HABIT_ICON]       ?: "⏱"
    val habitColorL = prefs[WidgetPreferencesKeys.HABIT_COLOR]      ?: 0xFF5A7A60L
    val running     = prefs[WidgetPreferencesKeys.TIMER_RUNNING]    ?: false
    val startEpoch  = prefs[WidgetPreferencesKeys.TIMER_START_EPOCH]  ?: 0L
    val prevElapsed = prefs[WidgetPreferencesKeys.TIMER_ELAPSED_SECS] ?: 0L
    val targetSecs  = prefs[WidgetPreferencesKeys.TIMER_TARGET_SECS]  ?: 0L
    val intensity   = prefs[WidgetPreferencesKeys.TIMER_INTENSITY]   ?: 3

    val habitColor = Color(habitColorL.toInt())

    // Compute elapsed at render time; passed back on stop so worker logs exactly this value.
    val nowEpoch   = System.currentTimeMillis() / 1000L
    val elapsed    = if (running && startEpoch > 0) nowEpoch - startEpoch + prevElapsed else prevElapsed
    val progress   = if (targetSecs > 0) (elapsed.toFloat() / targetSecs).coerceIn(0f, 1f) else 0f
    val nextIntensity = if (intensity >= 5) 1 else intensity + 1

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .cornerRadius(20.dp)
            .padding(10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = habitIcon, style = TextStyle(fontSize = 14.sp))
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text     = habitName,
                    style    = TextStyle(
                        color      = ColorProvider(Color.White),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                // Running indicator dot
                if (running) {
                    Box(
                        modifier = GlanceModifier
                            .size(7.dp)
                            .background(Color(0xFFEF5350))
                            .cornerRadius(4.dp),
                    ) {}
                }
            }

            Spacer(GlanceModifier.height(4.dp))

            // ── Elapsed time display ──────────────────────────────────────────
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = formatElapsed(elapsed),
                    style = TextStyle(
                        color      = ColorProvider(if (running) habitColor else Color.White),
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // ── Progress bar ──────────────────────────────────────────────────
            if (targetSecs > 0) {
                Spacer(GlanceModifier.height(3.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF2D3339))
                        .cornerRadius(2.dp),
                ) {
                    // Fill portion — approximated as a narrow inner Box
                    // (Glance doesn't support fractional widths, so we use a
                    //  ratio-based label instead for accuracy)
                }
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text  = "${(progress * 100).toInt()}% of goal",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFF9E9E9E)),
                        fontSize = 8.sp,
                    ),
                )
            }

            Spacer(GlanceModifier.defaultWeight())

            // ── Bottom row: Start/Stop + Intensity ────────────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Start / Stop button
                Box(
                    modifier = GlanceModifier
                        .height(26.dp)
                        .width(64.dp)
                        .background(if (running) Color(0xFFEF5350) else habitColor)
                        .cornerRadius(13.dp)
                        .clickable(
                            if (running) {
                                actionSendBroadcast(
                                    Intent(TimerActionReceiver.ACTION_TIMER_STOP).apply {
                                        setPackage(context.packageName)
                                        putExtra(TimerActionReceiver.EXTRA_HABIT_ID, habitId)
                                        putExtra(TimerActionReceiver.EXTRA_ELAPSED, elapsed)
                                        putExtra(TimerActionReceiver.EXTRA_INTENSITY, intensity)
                                        putExtra(TimerActionReceiver.EXTRA_DATE, today)
                                    }
                                )
                            } else {
                                actionSendBroadcast(
                                    Intent(TimerActionReceiver.ACTION_TIMER_START).apply {
                                        setPackage(context.packageName)
                                        putExtra(TimerActionReceiver.EXTRA_HABIT_ID, habitId)
                                    }
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = if (running) "⏹ Stop" else "▶ Start",
                        style = TextStyle(
                            color      = ColorProvider(Color.White),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                // Intensity dots — tap to cycle
                Row(
                    modifier = GlanceModifier.clickable(
                        actionSendBroadcast(
                            Intent(TimerActionReceiver.ACTION_TIMER_INTENSITY).apply {
                                setPackage(context.packageName)
                                putExtra(TimerActionReceiver.EXTRA_HABIT_ID, habitId)
                                putExtra(TimerActionReceiver.EXTRA_INTENSITY, nextIntensity)
                            }
                        )
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 1..5) {
                        Box(
                            modifier = GlanceModifier
                                .size(7.dp)
                                .background(
                                    if (i <= intensity) habitColor else Color(0xFF2D3339)
                                )
                                .cornerRadius(4.dp),
                        ) {}
                        if (i < 5) Spacer(GlanceModifier.width(2.dp))
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Formats seconds as HH:MM:SS (no leading zeros on hours if 0). */
private fun formatElapsed(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
