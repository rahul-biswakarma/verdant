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

/**
 * Root Glance composable for the habit contribution grid widget.
 *
 * Grid layout: 7 rows (Mon=0 … Sun=6) × [weeks] columns, oldest at the left.
 * Cell colour encodes completion intensity via [intensityColor].
 */
@androidx.compose.runtime.Composable
internal fun HabitGridContent() {
    val prefs = currentState<Preferences>()
    val size = LocalSize.current
    val context = LocalContext.current

    // ── State ─────────────────────────────────────────────────────────────────
    val habitId      = prefs[WidgetPreferencesKeys.HABIT_ID]      ?: return
    val habitName    = prefs[WidgetPreferencesKeys.HABIT_NAME]    ?: "Habit"
    val habitIcon    = prefs[WidgetPreferencesKeys.HABIT_ICON]    ?: "🌱"
    val habitColorL  = prefs[WidgetPreferencesKeys.HABIT_COLOR]   ?: 0xFF4CAF50L
    val streak       = prefs[WidgetPreferencesKeys.STREAK]        ?: 0
    val gridJson     = prefs[WidgetPreferencesKeys.GRID_JSON]     ?: "[]"
    val weekDone     = prefs[WidgetPreferencesKeys.WEEK_DONE]     ?: 0
    val weekTotal    = prefs[WidgetPreferencesKeys.WEEK_TOTAL]    ?: 7
    val isBinary     = prefs[WidgetPreferencesKeys.TRACKING_TYPE] == "BINARY"

    val habitColor = Color(habitColorL.toInt())
    val gridCells  = parseGridJson(gridJson)
    val today      = LocalDate.now()

    // ── Responsive sizing ─────────────────────────────────────────────────────
    val weeks = when {
        size.width < 200.dp  -> 4
        size.height < 150.dp -> 12
        else                 -> 20
    }
    val cellDp = when (weeks) {
        4    -> 18.dp
        12   -> 13.dp
        else -> 10.dp
    }

    // Align gridStart to the Monday of the earliest displayed week
    val todayDow   = today.dayOfWeek.value                        // 1=Mon..7=Sun
    val thisMonday = today.minusDays((todayDow - 1).toLong())
    val gridStart  = thisMonday.minusWeeks((weeks - 1).toLong())

    // ── Root container ────────────────────────────────────────────────────────
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .padding(8.dp)
            .cornerRadius(16.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon + name → tapping opens Habit Detail in the app
                Text(
                    text = "$habitIcon $habitName",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(
                            actionStartActivity(
                                Intent(context, Class.forName("com.verdant.app.MainActivity"))
                                    .apply {
                                        putExtra("habitId", habitId)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                            )
                        ),
                    maxLines = 1,
                )

                // Streak flame
                if (streak > 0) {
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = "🔥$streak",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFFB74D)),
                            fontSize = 10.sp,
                        ),
                    )
                }

                // Quick-check button (binary only) → logs today via QuickCheckReceiver
                if (isBinary) {
                    Spacer(GlanceModifier.width(4.dp))
                    Box(
                        modifier = GlanceModifier
                            .size(22.dp)
                            .background(habitColor.copy(alpha = 0.25f))
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
                            text = "✓",
                            style = TextStyle(
                                color = ColorProvider(habitColor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }

            Spacer(GlanceModifier.height(5.dp))

            // ── Contribution grid ─────────────────────────────────────────────
            // 7 rows (dayIndex 0=Mon … 6=Sun) × weeks columns (oldest → newest)
            Column {
                for (dayIndex in 0 until 7) {
                    Row {
                        for (weekIndex in 0 until weeks) {
                            val date     = gridStart.plusDays((weekIndex * 7 + dayIndex).toLong())
                            val isFuture = date.isAfter(today)
                            val isToday  = date == today

                            // Determine cell colour
                            val cellColor: Color = when {
                                isFuture -> Color.Transparent
                                isToday  -> {
                                    // Today always shows at least a faint tint as an indicator
                                    val raw = gridCells[date.toString()] ?: 0f
                                    intensityColor(habitColor, raw.coerceAtLeast(0.18f))
                                }
                                else -> intensityColor(habitColor, gridCells[date.toString()] ?: 0f)
                            }

                            val cellMod = GlanceModifier
                                .size(cellDp)
                                .padding(1.dp)
                                .background(cellColor)
                                .cornerRadius(2.dp)

                            // Past / today cells are tappable → deep link to Day Detail
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

            Spacer(GlanceModifier.height(5.dp))

            // ── Footer ───────────────────────────────────────────────────────
            Text(
                text = "$weekDone / $weekTotal this week",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF9E9E9E)),
                    fontSize = 9.sp,
                ),
            )
        }
    }
}
