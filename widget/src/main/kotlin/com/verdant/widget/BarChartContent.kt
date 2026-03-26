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

private const val MAX_BAR_WIDTH_DP = 120
private val BarBg = Color(0xFF1C1C1E)
private val BarMuted = Color(0xFF8E8E93)
private val BarTrack = Color(0xFF2C2C2E)
private val BarGreen = Color(0xFF34C759)
private val BarGreenDim = Color(0xFF1A5E2A)

/**
 * Bar chart widget (4x2).
 *
 * 7-day horizontal bars with bold typography, percentage labels,
 * and highlighted today row.
 */
@androidx.compose.runtime.Composable
internal fun BarChartContent() {
    val prefs = currentState<Preferences>()
    val json  = prefs[WidgetPreferencesKeys.BAR_CHART_JSON] ?: "[]"
    val bars  = parseBarChartJson(json)

    // Calculate totals for header
    val totalDone  = bars.sumOf { it.done }
    val totalAll   = bars.sumOf { it.total }
    val overallPct = if (totalAll > 0) (totalDone.toFloat() / totalAll * 100).toInt() else 0

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BarBg)
            .cornerRadius(20.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // ── Header ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "This Week",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = "$overallPct%",
                    style = TextStyle(
                        color = ColorProvider(
                            if (overallPct >= 80) BarGreen else BarMuted
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            if (bars.isEmpty()) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No data yet",
                        style = TextStyle(color = ColorProvider(BarMuted), fontSize = 10.sp),
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    for (bar in bars) {
                        BarRow(bar = bar)
                        Spacer(GlanceModifier.height(3.dp))
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun BarRow(bar: BarEntry) {
    val barColor = if (bar.isToday) BarGreen else BarGreenDim
    val barWidthDp = (bar.fraction * MAX_BAR_WIDTH_DP).toInt().coerceAtLeast(if (bar.total > 0) 3 else 0)
    val pct = if (bar.total > 0) (bar.fraction * 100).toInt() else 0

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Day label
        Text(
            text = bar.label,
            style = TextStyle(
                color = ColorProvider(if (bar.isToday) Color.White else BarMuted),
                fontSize = 10.sp,
                fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
            ),
            modifier = GlanceModifier.width(30.dp),
        )

        // Bar track
        Box(
            modifier = GlanceModifier
                .width(MAX_BAR_WIDTH_DP.dp)
                .height(8.dp)
                .background(BarTrack)
                .cornerRadius(4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (barWidthDp > 0) {
                Box(
                    modifier = GlanceModifier
                        .width(barWidthDp.dp)
                        .height(8.dp)
                        .background(barColor)
                        .cornerRadius(4.dp),
                ) {}
            }
        }

        Spacer(GlanceModifier.width(6.dp))

        // Percentage or count
        Text(
            text = if (bar.total > 0) "${bar.done}/${bar.total}" else "\u2014",
            style = TextStyle(
                color = ColorProvider(
                    if (bar.isToday && bar.fraction >= 1f) BarGreen else BarMuted
                ),
                fontSize = 9.sp,
                fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
            ),
            modifier = GlanceModifier.width(28.dp),
        )
    }
}

internal data class BarEntry(
    val label: String,
    val done: Int,
    val total: Int,
    val fraction: Float,
    val isToday: Boolean,
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
