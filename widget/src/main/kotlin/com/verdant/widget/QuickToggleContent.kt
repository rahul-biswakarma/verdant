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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate

/**
 * Quick-toggle widget UI (2×2).
 *
 * A large tap target covers the entire widget surface. Tapping sends
 * [ChecklistToggleReceiver.ACTION_TOGGLE] to flip the binary completion state.
 */
@Composable
internal fun QuickToggleContent() {
    val prefs     = currentState<Preferences>()
    val context   = LocalContext.current
    val today     = LocalDate.now().toString()

    val habitId   = prefs[WidgetPreferencesKeys.HABIT_ID]   ?: ""
    val habitName = prefs[WidgetPreferencesKeys.HABIT_NAME]  ?: "Habit"
    val habitIcon = prefs[WidgetPreferencesKeys.HABIT_ICON]  ?: "🌱"
    val colorLong = prefs[WidgetPreferencesKeys.HABIT_COLOR] ?: 0xFF4CAF50L
    val completed = prefs[WidgetPreferencesKeys.QUICK_TOGGLE_COMPLETED] ?: false

    val habitColor = Color(colorLong.toInt())
    val bgColor    = if (completed) habitColor else Color(0xFF1A1D21)
    val checkColor = if (completed) Color.White else habitColor

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .cornerRadius(16.dp)
            .clickable(
                actionSendBroadcast(
                    Intent(ChecklistToggleReceiver.ACTION_TOGGLE).apply {
                        setPackage(context.packageName)
                        putExtra(ChecklistToggleReceiver.EXTRA_HABIT_ID, habitId)
                        putExtra(ChecklistToggleReceiver.EXTRA_DATE, today)
                    }
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Completion ring
            Box(
                modifier = GlanceModifier
                    .size(48.dp)
                    .background(
                        if (completed) Color.White.copy(alpha = 0.2f) else habitColor.copy(alpha = 0.15f)
                    )
                    .cornerRadius(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (completed) {
                    Text(
                        text = "✓",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                } else {
                    Text(
                        text = habitIcon,
                        style = TextStyle(fontSize = 22.sp),
                    )
                }
            }

            Spacer(GlanceModifier.height(8.dp))

            Text(
                text = habitName,
                style = TextStyle(
                    color = ColorProvider(if (completed) Color.White else Color(0xFFE0E0E0)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
            )

            Spacer(GlanceModifier.height(4.dp))

            Text(
                text = if (completed) "Done!" else "Tap to complete",
                style = TextStyle(
                    color = ColorProvider(
                        if (completed) Color.White.copy(alpha = 0.8f) else Color(0xFF9E9E9E)
                    ),
                    fontSize = 9.sp,
                ),
            )
        }
    }
}
