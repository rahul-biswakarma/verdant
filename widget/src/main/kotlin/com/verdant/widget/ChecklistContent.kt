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
 * Checklist widget composable (4×2).
 *
 * Shows today's scheduled habits as a checkbox list. Each row broadcasts
 * [ChecklistToggleReceiver.ACTION_TOGGLE] to flip completion state.
 */
@androidx.compose.runtime.Composable
internal fun ChecklistContent() {
    val prefs   = currentState<Preferences>()
    val context = LocalContext.current
    val today   = LocalDate.now().toString()

    val checklistJson = prefs[WidgetPreferencesKeys.CHECKLIST_JSON] ?: "[]"
    val done          = prefs[WidgetPreferencesKeys.TODAY_DONE]     ?: 0
    val total         = prefs[WidgetPreferencesKeys.TODAY_TOTAL]    ?: 0

    val items = parseChecklistJson(checklistJson)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .padding(8.dp)
            .cornerRadius(16.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today's Habits",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = "$done/$total",
                    style = TextStyle(
                        color = ColorProvider(
                            if (done == total && total > 0) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(GlanceModifier.height(4.dp))

            // ── Habit rows ────────────────────────────────────────────────────
            if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No habits today",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF9E9E9E)),
                            fontSize = 10.sp,
                        ),
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    for (item in items.take(8)) {
                        ChecklistRow(item = item, today = today, context = context)
                        Spacer(GlanceModifier.height(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
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
        // Checkbox circle
        Box(
            modifier = GlanceModifier
                .size(18.dp)
                .background(
                    if (completed) habitColor else Color(0xFF2D3339)
                )
                .cornerRadius(9.dp)
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
            if (completed) {
                Text(
                    text = "✓",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        Spacer(GlanceModifier.width(6.dp))

        // Icon
        Text(
            text = item.icon,
            style = TextStyle(fontSize = 11.sp),
        )

        Spacer(GlanceModifier.width(4.dp))

        // Name
        Text(
            text = item.name,
            style = TextStyle(
                color = ColorProvider(
                    if (completed) Color(0xFF9E9E9E) else Color.White
                ),
                fontSize = 10.sp,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )

        // Status badge
        if (item.status.isNotEmpty()) {
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = item.status,
                style = TextStyle(
                    color = ColorProvider(habitColor),
                    fontSize = 8.sp,
                ),
            )
        }
    }
}

// ── Data / parsing ─────────────────────────────────────────────────────────────

internal data class ChecklistItem(
    val id: String,
    val icon: String,
    val name: String,
    val colorL: Long,
    val completed: Boolean,
    val status: String,
    val binary: Boolean,
)

internal fun parseChecklistJson(json: String): List<ChecklistItem> {
    if (json.isBlank() || json == "[]") return emptyList()
    return runCatching {
        val arr = JSONArray(json)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(ChecklistItem(
                    id        = obj.getString("id"),
                    icon      = obj.optString("icon", "🌱"),
                    name      = obj.getString("name"),
                    colorL    = obj.getLong("colorL"),
                    completed = obj.getBoolean("completed"),
                    status    = obj.optString("status", ""),
                    binary    = obj.optBoolean("binary", true),
                ))
            }
        }
    }.getOrElse { emptyList() }
}
