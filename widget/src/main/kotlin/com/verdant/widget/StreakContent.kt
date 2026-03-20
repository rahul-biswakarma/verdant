package com.verdant.widget

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
import org.json.JSONArray

/**
 * Streak widget composable (2×2).
 *
 * Shows top 3 active streaks and the all-time best habit streak.
 */
@androidx.compose.runtime.Composable
internal fun StreakContent() {
    val prefs = currentState<Preferences>()

    val topStreaksJson = prefs[WidgetPreferencesKeys.TOP_STREAKS_JSON]  ?: "[]"
    val bestName      = prefs[WidgetPreferencesKeys.BEST_EVER_NAME]    ?: ""
    val bestStreak    = prefs[WidgetPreferencesKeys.BEST_EVER_STREAK]  ?: 0
    val streaks       = parseStreaksJson(topStreaksJson)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .padding(8.dp)
            .cornerRadius(16.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text  = "🔥 Streaks",
                style = TextStyle(
                    color      = ColorProvider(Color.White),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(GlanceModifier.height(4.dp))

            if (streaks.isEmpty()) {
                Text(
                    text  = "No active streaks",
                    style = TextStyle(color = ColorProvider(Color(0xFF9E9E9E)), fontSize = 9.sp),
                )
            } else {
                for (item in streaks) {
                    StreakRow(item = item)
                    Spacer(GlanceModifier.height(3.dp))
                }
            }

            Spacer(GlanceModifier.height(4.dp))

            // ── Divider ────────────────────────────────────────────────────────
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF2D3339)),
            ) {}

            Spacer(GlanceModifier.height(4.dp))

            // ── Best ever ─────────────────────────────────────────────────────
            if (bestStreak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "⭐",
                        style = TextStyle(fontSize = 9.sp),
                    )
                    Spacer(GlanceModifier.width(3.dp))
                    Text(
                        text  = "Best",
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFF9E9E9E)),
                            fontSize = 9.sp,
                        ),
                    )
                    Spacer(GlanceModifier.width(2.dp))
                    Text(
                        text     = bestName,
                        style    = TextStyle(
                            color    = ColorProvider(Color(0xFF9E9E9E)),
                            fontSize = 9.sp,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text  = "$bestStreak",
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

@androidx.compose.runtime.Composable
private fun StreakRow(item: StreakItem) {
    val color = Color(item.colorL.toInt())

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Colored dot
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(color)
                .cornerRadius(4.dp),
        ) {}

        Spacer(GlanceModifier.width(4.dp))

        // Icon + name
        Text(
            text     = "${item.icon} ${item.name}",
            style    = TextStyle(
                color    = ColorProvider(Color.White),
                fontSize = 9.sp,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )

        // Streak count
        Text(
            text  = "🔥${item.streak}",
            style = TextStyle(
                color      = ColorProvider(Color(0xFFFFB74D)),
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

// ── Data / parsing ─────────────────────────────────────────────────────────────

internal data class StreakItem(
    val icon:   String,
    val name:   String,
    val colorL: Long,
    val streak: Int,
)

internal fun parseStreaksJson(json: String): List<StreakItem> {
    if (json.isBlank() || json == "[]") return emptyList()
    return runCatching {
        val arr = JSONArray(json)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(StreakItem(
                    icon   = obj.optString("icon", "🌱"),
                    name   = obj.getString("name"),
                    colorL = obj.getLong("colorL"),
                    streak = obj.getInt("streak"),
                ))
            }
        }
    }.getOrElse { emptyList() }
}
