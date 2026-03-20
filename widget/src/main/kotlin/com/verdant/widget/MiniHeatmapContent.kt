package com.verdant.widget

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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

private const val MINI_WEEKS     = 8
private const val MINI_CELL_DP   = 10   // cell size
private const val MINI_PADDING   = 1    // gap between cells

/**
 * Mini heatmap + stats widget composable (4×2).
 *
 * Left: 8-week contribution grid (same algo as [HabitGridContent]).
 * Right: stats column — streak, best, completion %, trend, quick-log button.
 */
@androidx.compose.runtime.Composable
internal fun MiniHeatmapContent() {
    val prefs   = currentState<Preferences>()
    val context = LocalContext.current

    val habitId    = prefs[WidgetPreferencesKeys.HABIT_ID]      ?: return
    val habitName  = prefs[WidgetPreferencesKeys.HABIT_NAME]    ?: "Habit"
    val habitIcon  = prefs[WidgetPreferencesKeys.HABIT_ICON]    ?: "🌱"
    val habitColorL = prefs[WidgetPreferencesKeys.HABIT_COLOR]  ?: 0xFF4CAF50L
    val streak     = prefs[WidgetPreferencesKeys.STREAK]        ?: 0
    val best       = prefs[WidgetPreferencesKeys.BEST_STREAK]   ?: 0
    val rate       = prefs[WidgetPreferencesKeys.COMPLETION_RATE] ?: 0f
    val trend      = prefs[WidgetPreferencesKeys.TREND_PCT]     ?: 0f
    val gridJson   = prefs[WidgetPreferencesKeys.GRID_JSON]     ?: "[]"
    val isBinary   = prefs[WidgetPreferencesKeys.TRACKING_TYPE] == "BINARY"

    val habitColor = Color(habitColorL.toInt())
    val gridCells  = parseGridJson(gridJson)
    val today      = LocalDate.now()

    val todayDow   = today.dayOfWeek.value
    val thisMonday = today.minusDays((todayDow - 1).toLong())
    val gridStart  = thisMonday.minusWeeks((MINI_WEEKS - 1).toLong())

    val trendColor = when {
        trend > 0  -> Color(0xFF4CAF50)
        trend < 0  -> Color(0xFFEF5350)
        else       -> Color(0xFF9E9E9E)
    }
    val trendArrow = when {
        trend > 0  -> "↑"
        trend < 0  -> "↓"
        else       -> "→"
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .padding(8.dp)
            .cornerRadius(16.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Row(modifier = GlanceModifier.fillMaxSize()) {

            // ── Left: 8-week grid ──────────────────────────────────────────────
            Column(modifier = GlanceModifier.fillMaxHeight()) {
                // Habit label
                Text(
                    text = "$habitIcon $habitName",
                    style = TextStyle(
                        color      = ColorProvider(Color.White),
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )

                Spacer(GlanceModifier.height(3.dp))

                // Grid: 7 rows × 8 cols
                Column {
                    for (dayIndex in 0 until 7) {
                        Row {
                            for (weekIndex in 0 until MINI_WEEKS) {
                                val date     = gridStart.plusDays((weekIndex * 7 + dayIndex).toLong())
                                val isFuture = date.isAfter(today)
                                val isToday  = date == today
                                val raw      = gridCells[date.toString()] ?: 0f

                                val cellColor: Color = when {
                                    isFuture -> Color.Transparent
                                    isToday  -> intensityColor(habitColor, raw.coerceAtLeast(0.18f))
                                    else     -> intensityColor(habitColor, raw)
                                }

                                Box(
                                    modifier = GlanceModifier
                                        .size(MINI_CELL_DP.dp)
                                        .padding(MINI_PADDING.dp)
                                        .background(cellColor)
                                        .cornerRadius(2.dp),
                                ) {}
                            }
                        }
                    }
                }
            }

            Spacer(GlanceModifier.width(8.dp))

            // ── Right: stats column ───────────────────────────────────────────
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight(),
            ) {
                StatRow(label = "🔥", value = "$streak day streak", color = Color(0xFFFFB74D))
                Spacer(GlanceModifier.height(3.dp))
                StatRow(label = "⭐", value = "Best $best", color = Color(0xFF9E9E9E))
                Spacer(GlanceModifier.height(3.dp))
                StatRow(label = "📊", value = "${(rate * 100).toInt()}% this month", color = Color(0xFF9E9E9E))
                Spacer(GlanceModifier.height(3.dp))
                StatRow(
                    label = trendArrow,
                    value = "${kotlin.math.abs(trend).toInt()}% vs prev month",
                    color = trendColor,
                )

                Spacer(GlanceModifier.height(4.dp))

                // Quick-log button (binary habits only)
                if (isBinary) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .background(habitColor.copy(alpha = 0.22f))
                            .cornerRadius(10.dp)
                            .clickable(
                                actionSendBroadcast(
                                    Intent(QuickCheckReceiver.ACTION_QUICK_CHECK).apply {
                                        setPackage(context.packageName)
                                        putExtra(QuickCheckReceiver.EXTRA_HABIT_ID, habitId)
                                        putExtra(QuickCheckReceiver.EXTRA_DATE, today.toString())
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = "✓ Log today",
                            style = TextStyle(
                                color      = ColorProvider(habitColor),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = TextStyle(fontSize = 9.sp))
        Spacer(GlanceModifier.width(3.dp))
        Text(
            text  = value,
            style = TextStyle(color = ColorProvider(color), fontSize = 9.sp),
            maxLines = 1,
        )
    }
}
