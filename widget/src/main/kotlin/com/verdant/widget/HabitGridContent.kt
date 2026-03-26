package com.verdant.widget

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
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

private val GridBg = Color(0xFF1C1C1E)
private val GridMuted = Color(0xFF8E8E93)

/**
 * Habit contribution grid widget (2x2 -> 4x3).
 *
 * Shows bold Total/Best stats header, then a GitHub-style contribution grid
 * with color-coded intensity cells.
 */
@androidx.compose.runtime.Composable
internal fun HabitGridContent() {
    val prefs = currentState<Preferences>()
    val size = LocalSize.current
    val context = LocalContext.current
    val habitId      = prefs[WidgetPreferencesKeys.HABIT_ID]      ?: return
    val habitName    = prefs[WidgetPreferencesKeys.HABIT_NAME]    ?: "Habit"
    val habitIcon    = prefs[WidgetPreferencesKeys.HABIT_ICON]    ?: "\uD83C\uDF31"
    val habitColorL  = prefs[WidgetPreferencesKeys.HABIT_COLOR]   ?: 0xFF4CAF50L
    val streak       = prefs[WidgetPreferencesKeys.STREAK]        ?: 0
    val gridJson     = prefs[WidgetPreferencesKeys.GRID_JSON]     ?: "[]"
    val weekDone     = prefs[WidgetPreferencesKeys.WEEK_DONE]     ?: 0
    val weekTotal    = prefs[WidgetPreferencesKeys.WEEK_TOTAL]    ?: 7
    val isBinary     = prefs[WidgetPreferencesKeys.TRACKING_TYPE] == "BINARY"
    val totalCompletions = prefs[WidgetPreferencesKeys.TOTAL_COMPLETIONS] ?: 0
    val bestStreak       = prefs[WidgetPreferencesKeys.BEST_EVER_STREAK_SINGLE] ?: streak

    val habitColor = Color(habitColorL.toInt())
    val gridCells  = parseGridJson(gridJson)
    val today      = LocalDate.now()
    val weeks = when {
        size.width < 200.dp  -> 6
        size.height < 150.dp -> 12
        else                 -> 16
    }
    val cellDp = when (weeks) {
        6    -> 14.dp
        12   -> 11.dp
        else -> 9.dp
    }
    val gapDp = 2.dp

    val todayDow   = today.dayOfWeek.value
    val thisMonday = today.minusDays((todayDow - 1).toLong())
    val gridStart  = thisMonday.minusWeeks((weeks - 1).toLong())

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GridBg)
            .cornerRadius(20.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // ── Header: Bold stats ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Total completions
                Text(
                    text = "$totalCompletions",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.width(3.dp))
                Text(
                    text = "Total",
                    style = TextStyle(
                        color = ColorProvider(GridMuted),
                        fontSize = 11.sp,
                    ),
                )

                Spacer(GlanceModifier.width(12.dp))

                // Best streak
                Text(
                    text = "$bestStreak",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.width(3.dp))
                Text(
                    text = "Best",
                    style = TextStyle(
                        color = ColorProvider(GridMuted),
                        fontSize = 11.sp,
                    ),
                )

                Spacer(GlanceModifier.defaultWeight())

                // Habit icon + name
                Text(
                    text = "$habitIcon",
                    style = TextStyle(fontSize = 14.sp),
                    modifier = GlanceModifier.clickable(
                        actionStartActivity(
                            Intent(context, Class.forName("com.verdant.app.MainActivity"))
                                .apply {
                                    putExtra("habitId", habitId)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                        )
                    ),
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            // ── Contribution grid: 7 rows x N cols ──
            Column {
                for (dayIndex in 0 until 7) {
                    Row {
                        for (weekIndex in 0 until weeks) {
                            val date     = gridStart.plusDays((weekIndex * 7 + dayIndex).toLong())
                            val isFuture = date.isAfter(today)
                            val isToday  = date == today

                            val cellColor: Color = when {
                                isFuture -> Color.Transparent
                                isToday  -> {
                                    val raw = gridCells[date.toString()] ?: 0f
                                    intensityColor(habitColor, raw.coerceAtLeast(0.18f))
                                }
                                else -> intensityColor(habitColor, gridCells[date.toString()] ?: 0f)
                            }

                            val cellMod = GlanceModifier
                                .size(cellDp)
                                .padding(1.dp)
                                .background(cellColor)
                                .cornerRadius(3.dp)

                            val clickableMod = if (!isFuture) {
                                cellMod.clickable(
                                    actionStartActivity(
                                        Intent(context, Class.forName("com.verdant.app.MainActivity"))
                                            .apply {
                                                putExtra("date", date.toString())
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            }
                                    )
                                )
                            } else {
                                cellMod
                            }

                            Box(modifier = clickableMod) {}
                        }
                    }
                }
            }

            Spacer(GlanceModifier.height(6.dp))

            // ── Footer: streak + week progress ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (streak > 0) {
                    Text(
                        text = "\uD83D\uDD25 $streak",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFF9F0A)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                }

                Text(
                    text = "$weekDone/$weekTotal this week",
                    style = TextStyle(
                        color = ColorProvider(GridMuted),
                        fontSize = 9.sp,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )

                // Quick-check button (binary only)
                if (isBinary) {
                    Box(
                        modifier = GlanceModifier
                            .size(22.dp)
                            .background(habitColor.copy(alpha = 0.2f))
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
                            text = "\u2713",
                            style = TextStyle(
                                color = ColorProvider(habitColor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
