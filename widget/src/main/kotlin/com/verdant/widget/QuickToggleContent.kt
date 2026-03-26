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
 * Quick-toggle binary habit widget composable (2×2).
 *
 * The entire card is tappable — tapping fires [QuickCheckReceiver] to log
 * a binary entry for today.  The visual state (done vs pending) is driven
 * by [WidgetPreferencesKeys.TOGGLE_COMPLETED].
 */
@Composable
internal fun QuickToggleContent() {
    val prefs     = currentState<Preferences>()
    val context   = LocalContext.current
    val today     = LocalDate.now().toString()

    val habitId   = prefs[WidgetPreferencesKeys.HABIT_ID]      ?: ""
    val habitName = prefs[WidgetPreferencesKeys.HABIT_NAME]    ?: "Habit"
    val habitIcon = prefs[WidgetPreferencesKeys.HABIT_ICON]    ?: "🌱"
    val habitColorL = prefs[WidgetPreferencesKeys.HABIT_COLOR] ?: 0xFF5A7A60L
    val streak    = prefs[WidgetPreferencesKeys.STREAK]        ?: 0
    val completed = prefs[WidgetPreferencesKeys.TOGGLE_COMPLETED] ?: false

    val habitColor   = Color(habitColorL.toInt())
    val bgColor      = if (completed) habitColor.copy(alpha = 0.18f) else Color(0xFF1A1D21)
    val accentColor  = if (completed) habitColor else Color(0xFF2D3748)
    val labelColor   = if (completed) habitColor else Color(0xFF9E9E9E)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .cornerRadius(20.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Tappable inner card — fires QuickCheckReceiver
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(18.dp)
                .padding(10.dp)
                .clickable(
                    actionSendBroadcast(
                        Intent(QuickCheckReceiver.ACTION_QUICK_CHECK).apply {
                            setPackage(context.packageName)
                            putExtra(QuickCheckReceiver.EXTRA_HABIT_ID, habitId)
                            putExtra(QuickCheckReceiver.EXTRA_DATE, today)
                        }
                    )
                ),
            contentAlignment = Alignment.TopStart,
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {

                // ── Icon row ──────────────────────────────────────────────────
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = habitIcon, style = TextStyle(fontSize = 22.sp))
                    Spacer(GlanceModifier.defaultWeight())
                    // Completion badge
                    Box(
                        modifier = GlanceModifier
                            .size(22.dp)
                            .background(accentColor)
                            .cornerRadius(11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (completed) {
                            Text(
                                text  = "✓",
                                style = TextStyle(
                                    color      = ColorProvider(Color.White),
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }

                Spacer(GlanceModifier.defaultWeight())

                // ── Habit name ────────────────────────────────────────────────
                Text(
                    text     = habitName,
                    style    = TextStyle(
                        color      = ColorProvider(Color.White),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )

                Spacer(GlanceModifier.height(4.dp))

                // ── Status / streak row ────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = if (completed) "✓ Done today" else "Tap to complete",
                        style = TextStyle(
                            color    = ColorProvider(labelColor),
                            fontSize = 9.sp,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    if (streak > 0) {
                        Text(
                            text  = "🔥$streak",
                            style = TextStyle(
                                color      = ColorProvider(Color(0xFFFFB74D)),
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
