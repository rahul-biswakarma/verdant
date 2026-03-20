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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.json.JSONArray

private const val MAX_BAR_WIDTH_DP = 130   // dp available for the bar itself
private const val LABEL_WIDTH_DP   = 28    // day-of-week label
private const val COUNT_WIDTH_DP   = 26    // "done/total" count

/**
 * Bar chart widget composable (4×2).
 *
 * Reads [WidgetPreferencesKeys.BAR_CHART_JSON] and renders 7 horizontal bars
 * (oldest → today) using pure Glance Row+Box composables — no Canvas required.
 */
@androidx.compose.runtime.Composable
internal fun BarChartContent() {
    val prefs = currentState<Preferences>()
    val json  = prefs[WidgetPreferencesKeys.BAR_CHART_JSON] ?: "[]"
    val bars  = parseBarChartJson(json)

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
                text  = "This Week",
                style = TextStyle(
                    color      = ColorProvider(Color.White),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(GlanceModifier.height(6.dp))

            if (bars.isEmpty()) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data", style = TextStyle(color = ColorProvider(Color(0xFF9E9E9E)), fontSize = 10.sp))
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    for (bar in bars) {
                        BarRow(bar = bar)
                        Spacer(GlanceModifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun BarRow(bar: BarEntry) {
    val barColor = if (bar.isToday) Color(0xFF4CAF50) else Color(0xFF3D7A42)
    val barWidthDp = (bar.fraction * MAX_BAR_WIDTH_DP).toInt().coerceAtLeast(if (bar.total > 0) 2 else 0)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Day label
        Text(
            text     = bar.label,
            style    = TextStyle(
                color    = ColorProvider(
                    if (bar.isToday) Color.White else Color(0xFF9E9E9E)
                ),
                fontSize = 9.sp,
                fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
            ),
            modifier = GlanceModifier.width(LABEL_WIDTH_DP.dp),
        )

        // Bar track
        Box(
            modifier = GlanceModifier
                .width(MAX_BAR_WIDTH_DP.dp)
                .height(7.dp)
                .background(Color(0xFF2D3339))
                .cornerRadius(4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (barWidthDp > 0) {
                Box(
                    modifier = GlanceModifier
                        .width(barWidthDp.dp)
                        .height(7.dp)
                        .background(barColor)
                        .cornerRadius(4.dp),
                ) {}
            }
        }

        Spacer(GlanceModifier.width(4.dp))

        // Count
        Text(
            text  = if (bar.total > 0) "${bar.done}/${bar.total}" else "—",
            style = TextStyle(
                color    = ColorProvider(Color(0xFF9E9E9E)),
                fontSize = 8.sp,
            ),
            modifier = GlanceModifier.width(COUNT_WIDTH_DP.dp),
        )
    }
}

// ── Data / parsing ─────────────────────────────────────────────────────────────

internal data class BarEntry(
    val label:    String,
    val done:     Int,
    val total:    Int,
    val fraction: Float,
    val isToday:  Boolean,
)

internal fun parseBarChartJson(json: String): List<BarEntry> {
    if (json.isBlank() || json == "[]") return emptyList()
    return runCatching {
        val arr = JSONArray(json)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(BarEntry(
                    label    = obj.getString("label"),
                    done     = obj.getInt("done"),
                    total    = obj.getInt("total"),
                    fraction = obj.getDouble("fraction").toFloat(),
                    isToday  = obj.getBoolean("today"),
                ))
            }
        }
    }.getOrElse { emptyList() }
}
