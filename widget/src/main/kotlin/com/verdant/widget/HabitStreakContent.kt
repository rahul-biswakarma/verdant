package com.verdant.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
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
import kotlin.math.roundToInt

/**
 * Per-habit streak widget UI (2×2).
 *
 * Displays: habit icon + name, current streak count (large), a segmented
 * progress bar showing 30-day completion rate, and the all-time best streak.
 */
@Composable
internal fun HabitStreakContent() {
    val prefs = currentState<Preferences>()

    val habitName      = prefs[WidgetPreferencesKeys.HABIT_NAME]    ?: "Habit"
    val habitIcon      = prefs[WidgetPreferencesKeys.HABIT_ICON]    ?: "🌱"
    val colorLong      = prefs[WidgetPreferencesKeys.HABIT_COLOR]   ?: 0xFF4CAF50L
    val streak         = prefs[WidgetPreferencesKeys.STREAK]        ?: 0
    val bestStreak     = prefs[WidgetPreferencesKeys.BEST_STREAK]   ?: 0
    val completionRate = prefs[WidgetPreferencesKeys.COMPLETION_RATE] ?: 0f

    val habitColor = Color(colorLong.toInt())
    val filledDots = (completionRate * 7).roundToInt().coerceIn(0, 7)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .cornerRadius(16.dp)
            .padding(12.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // Header: icon + name
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = habitIcon,
                    style = TextStyle(fontSize = 14.sp),
                )
                Spacer(GlanceModifier.width(5.dp))
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

            Spacer(GlanceModifier.height(4.dp))

            // Flame + streak count
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥",
                        style = TextStyle(fontSize = 22.sp),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = streak.toString(),
                        style = TextStyle(
                            color = ColorProvider(habitColor),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Text(
                text = "day streak",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF9E9E9E)),
                    fontSize = 9.sp,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )

            Spacer(GlanceModifier.height(6.dp))

            // 7-dot progress bar (represents last 7 days)
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val filled = i < filledDots
                    Box(
                        modifier = GlanceModifier
                            .size(10.dp)
                            .background(if (filled) habitColor else Color(0xFF2D3339))
                            .cornerRadius(5.dp),
                    ) {}
                    if (i < 6) Spacer(GlanceModifier.width(3.dp))
                }
            }

            Spacer(GlanceModifier.height(4.dp))

            // Best streak
            Text(
                text = "Best: $bestStreak days",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF757575)),
                    fontSize = 9.sp,
                ),
            )
        }
    }
}
