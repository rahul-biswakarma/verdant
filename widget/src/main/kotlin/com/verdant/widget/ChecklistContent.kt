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

private val CheckBg = Color(0xFF1C1C1E)
private val CheckMuted = Color(0xFF8E8E93)
private val CheckGreen = Color(0xFF34C759)
private val CheckDivider = Color(0xFF2C2C2E)

/**
 * Checklist widget (4x2).
 *
 * Shows today's habits as a polished dark checkbox list with
 * completion fraction and progress bar at top.
 */
@Composable
internal fun ChecklistContent() {
    val prefs   = currentState<Preferences>()
    val context = LocalContext.current
    val today   = LocalDate.now().toString()

    val checklistJson = prefs[WidgetPreferencesKeys.CHECKLIST_JSON] ?: "[]"
    val done          = prefs[WidgetPreferencesKeys.TODAY_DONE]     ?: 0
    val total         = prefs[WidgetPreferencesKeys.TODAY_TOTAL]    ?: 0

    val items = parseChecklistJson(checklistJson)
    val fraction = if (total > 0) done.toFloat() / total else 0f
    val pct = (fraction * 100).toInt()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CheckBg)
            .cornerRadius(20.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // ── Header: Title + fraction ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )

                // Done/Total pill
                Box(
                    modifier = GlanceModifier
                        .background(
                            if (done == total && total > 0) CheckGreen.copy(alpha = 0.18f)
                            else Color(0xFF2C2C2E)
                        )
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "$done/$total",
                        style = TextStyle(
                            color = ColorProvider(
                                if (done == total && total > 0) CheckGreen else Color.White
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(GlanceModifier.height(6.dp))

            // ── Progress bar ──
            ProgressBar(fraction = fraction, color = CheckGreen, height = 4)

            Spacer(GlanceModifier.height(8.dp))

            // ── Habit rows ──
            if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No habits scheduled today",
                        style = TextStyle(color = ColorProvider(CheckMuted), fontSize = 10.sp),
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    for ((index, item) in items.take(7).withIndex()) {
                        if (index > 0) {
                            // Subtle divider
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(CheckDivider),
                            ) {}
                            Spacer(GlanceModifier.height(2.dp))
                        }
                        ChecklistRow(item = item, today = today, context = context)
                        Spacer(GlanceModifier.height(2.dp))
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
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Checkbox circle
        Box(
            modifier = GlanceModifier
                .size(20.dp)
                .background(
                    if (completed) CheckGreen else Color(0xFF3A3A3C)
                )
                .cornerRadius(10.dp)
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
                    text = "\u2713",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        Spacer(GlanceModifier.width(8.dp))

        // Icon
        Text(
            text = item.icon,
            style = TextStyle(fontSize = 13.sp),
        )

        Spacer(GlanceModifier.width(6.dp))

        // Name
        Text(
            text = item.name,
            style = TextStyle(
                color = ColorProvider(
                    if (completed) CheckMuted else Color.White
                ),
                fontSize = 11.sp,
                fontWeight = if (!completed) FontWeight.Bold else FontWeight.Normal,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )

        // Status indicator
        if (item.status.isNotEmpty()) {
            Box(
                modifier = GlanceModifier
                    .background(
                        if (completed) CheckGreen.copy(alpha = 0.12f)
                        else habitColor.copy(alpha = 0.12f)
                    )
                    .cornerRadius(6.dp)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = item.status,
                    style = TextStyle(
                        color = ColorProvider(if (completed) CheckGreen else habitColor),
                        fontSize = 8.sp,
                    ),
                )
            }
        }
    }
}

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
                    icon      = obj.optString("icon", "\uD83C\uDF31"),
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
