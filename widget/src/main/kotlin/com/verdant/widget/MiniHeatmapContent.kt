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

private const val MINI_WEEKS   = 8
private const val MINI_CELL_DP = 10

private val HeatmapBg = Color(0xFF1C1C1E)
private val HeatmapMuted = Color(0xFF8E8E93)

/**
 * Mini heatmap + stats widget (4x2).
 *
 * Left: 8-week contribution grid with rounded cells.
 * Right: bold stats column with streak, best, rate, trend, and quick-log.
 */
@androidx.compose.runtime.Composable
internal fun MiniHeatmapContent() {
    val prefs   = currentState<Preferences>()
    val context = LocalContext.current

    val habitId     = prefs[WidgetPreferencesKeys.HABIT_ID]        ?: return
    val habitName   = prefs[WidgetPreferencesKeys.HABIT_NAME]      ?: "Habit"
    val habitIcon   = prefs[WidgetPreferencesKeys.HABIT_ICON]      ?: "\uD83C\uDF31"
    val habitColorL = prefs[WidgetPreferencesKeys.HABIT_COLOR]     ?: 0xFF4CAF50L
    val streak      = prefs[WidgetPreferencesKeys.STREAK]          ?: 0
    val best        = prefs[WidgetPreferencesKeys.BEST_STREAK]     ?: 0
    val rate        = prefs[WidgetPreferencesKeys.COMPLETION_RATE] ?: 0f
    val trend       = prefs[WidgetPreferencesKeys.TREND_PCT]       ?: 0f
    val gridJson    = prefs[WidgetPreferencesKeys.GRID_JSON]       ?: "[]"
    val isBinary    = prefs[WidgetPreferencesKeys.TRACKING_TYPE] == "BINARY"

    val habitColor = Color(habitColorL.toInt())
    val gridCells  = parseGridJson(gridJson)
    val today      = LocalDate.now()

    val todayDow   = today.dayOfWeek.value
    val thisMonday = today.minusDays((todayDow - 1).toLong())
    val gridStart  = thisMonday.minusWeeks((MINI_WEEKS - 1).toLong())

    val trendColor = when {
        trend > 0  -> Color(0xFF34C759)
        trend < 0  -> Color(0xFFFF453A)
        else       -> HeatmapMuted
    }
    val trendArrow = when {
        trend > 0  -> "\u2191"
        trend < 0  -> "\u2193"
        else       -> "\u2192"
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(HeatmapBg)
            .cornerRadius(20.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // ── Header: icon + name ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$habitIcon $habitName",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )

                if (streak > 0) {
                    Box(
                        modifier = GlanceModifier
                            .background(Color(0xFF2C1215))
                            .cornerRadius(8.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "\uD83D\uDD25 $streak",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFFF9F0A)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }

            Spacer(GlanceModifier.height(6.dp))

            // ── Body: grid + stats side by side ──
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                // Grid: 7 rows x 8 cols
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
                                        .padding(1.dp)
                                        .background(cellColor)
                                        .cornerRadius(2.dp),
                                ) {}
                            }
                        }
                    }
                }

                Spacer(GlanceModifier.width(10.dp))

                // Stats column
                Column(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                ) {
                    // Streak + Best row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                text = "STREAK",
                                style = TextStyle(color = ColorProvider(HeatmapMuted), fontSize = 7.sp),
                            )
                            Text(
                                text = "$streak",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Column {
                            Text(
                                text = "BEST",
                                style = TextStyle(color = ColorProvider(HeatmapMuted), fontSize = 7.sp),
                            )
                            Text(
                                text = "$best",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFFFD60A)),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(4.dp))

                    // Completion rate
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${(rate * 100).toInt()}%",
                            style = TextStyle(
                                color = ColorProvider(habitColor),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(GlanceModifier.width(4.dp))
                        Text(
                            text = "rate",
                            style = TextStyle(color = ColorProvider(HeatmapMuted), fontSize = 9.sp),
                        )
                    }

                    Spacer(GlanceModifier.height(2.dp))

                    // Trend
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$trendArrow ${kotlin.math.abs(trend).toInt()}%",
                            style = TextStyle(
                                color = ColorProvider(trendColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(GlanceModifier.width(3.dp))
                        Text(
                            text = "vs last",
                            style = TextStyle(color = ColorProvider(HeatmapMuted), fontSize = 8.sp),
                        )
                    }

                    Spacer(GlanceModifier.defaultWeight())

                    // Quick-log button
                    if (isBinary) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(habitColor.copy(alpha = 0.18f))
                                .cornerRadius(11.dp)
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
                                text = "\u2713 Log",
                                style = TextStyle(
                                    color = ColorProvider(habitColor),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
