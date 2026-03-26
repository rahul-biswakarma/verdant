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
import org.json.JSONArray
import java.time.LocalDate

/**
 * Multi-habit widget composable (4×2).
 *
 * Renders up to 5 habit rows sourced from [WidgetPreferencesKeys.MULTI_HABIT_JSON]
 * (same JSON schema as CHECKLIST_JSON).  Each row's toggle fires
 * [ChecklistToggleReceiver] — the existing toggle/log path.
 */
@Composable
internal fun MultiHabitContent() {
    val prefs   = currentState<Preferences>()
    val context = LocalContext.current
    val today   = LocalDate.now().toString()

    val multiJson = prefs[WidgetPreferencesKeys.MULTI_HABIT_JSON] ?: "[]"
    val done      = prefs[WidgetPreferencesKeys.TODAY_DONE]       ?: 0
    val total     = prefs[WidgetPreferencesKeys.TODAY_TOTAL]      ?: 0

    val items = parseChecklistJson(multiJson)  // reuse same data model + parser

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .cornerRadius(16.dp)
            .padding(10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "My Habits",
                    style = TextStyle(
                        color      = ColorProvider(Color.White),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
                // Completion pill
                Box(
                    modifier = GlanceModifier
                        .background(
                            if (done == total && total > 0) Color(0xFF2D4A30) else Color(0xFF2D3339)
                        )
                        .cornerRadius(8.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text  = "$done / $total",
                        style = TextStyle(
                            color      = ColorProvider(
                                if (done == total && total > 0) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                            ),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(GlanceModifier.height(6.dp))

            // ── Divider ───────────────────────────────────────────────────────
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF2D3339)),
            ) {}

            Spacer(GlanceModifier.height(4.dp))

            // ── Habit rows ────────────────────────────────────────────────────
            if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "No habits selected",
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFF9E9E9E)),
                            fontSize = 10.sp,
                        ),
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    for (item in items.take(5)) {
                        MultiHabitRow(item = item, today = today, context = context)
                        Spacer(GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiHabitRow(
    item: ChecklistItem,
    today: String,
    context: android.content.Context,
) {
    val habitColor = Color(item.colorL.toInt())
    val completed  = item.completed

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Colored dot accent
        Box(
            modifier = GlanceModifier
                .size(6.dp)
                .background(habitColor)
                .cornerRadius(3.dp),
        ) {}

        Spacer(GlanceModifier.width(5.dp))

        // Icon
        Text(text = item.icon, style = TextStyle(fontSize = 12.sp))

        Spacer(GlanceModifier.width(5.dp))

        // Name (with strike-through style via color when done)
        Text(
            text     = item.name,
            style    = TextStyle(
                color    = ColorProvider(if (completed) Color(0xFF6B7280) else Color.White),
                fontSize = 10.sp,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )

        Spacer(GlanceModifier.width(6.dp))

        // Toggle button
        Box(
            modifier = GlanceModifier
                .height(22.dp)
                .width(52.dp)
                .background(
                    if (completed) habitColor.copy(alpha = 0.2f) else habitColor
                )
                .cornerRadius(11.dp)
                .clickable(
                    actionSendBroadcast(
                        Intent(ChecklistToggleReceiver.ACTION_TOGGLE).apply {
                            setPackage(context.packageName)
                            putExtra(ChecklistToggleReceiver.EXTRA_HABIT_ID, item.id)
                            putExtra(ChecklistToggleReceiver.EXTRA_DATE, today)
                        }
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = if (completed) "✓ Done" else "Tap",
                style = TextStyle(
                    color      = ColorProvider(
                        if (completed) habitColor else Color.White
                    ),
                    fontSize   = 8.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}
