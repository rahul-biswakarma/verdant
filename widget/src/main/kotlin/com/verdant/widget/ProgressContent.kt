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

/**
 * Progress widget composable (2×2).
 *
 * Adapts display based on [WidgetPreferencesKeys.TRACKING_TYPE]:
 *  - BINARY      → streak + "X day streak" label, green checkmark when done
 *  - QUANTITATIVE → "value / target unit" with segmented bar
 *  - DURATION    → "MM:SS / target" with segmented bar
 */
@Composable
internal fun ProgressContent() {
    val prefs = currentState<Preferences>()

    val habitName    = prefs[WidgetPreferencesKeys.HABIT_NAME]      ?: "Habit"
    val habitIcon    = prefs[WidgetPreferencesKeys.HABIT_ICON]      ?: "🌱"
    val habitColorL  = prefs[WidgetPreferencesKeys.HABIT_COLOR]     ?: 0xFF5A7A60L
    val trackingType = prefs[WidgetPreferencesKeys.TRACKING_TYPE]   ?: "BINARY"
    val streak       = prefs[WidgetPreferencesKeys.STREAK]          ?: 0
    val value        = prefs[WidgetPreferencesKeys.PROGRESS_VALUE]  ?: 0f
    val target       = prefs[WidgetPreferencesKeys.PROGRESS_TARGET] ?: 0f
    val unit         = prefs[WidgetPreferencesKeys.PROGRESS_UNIT]   ?: ""

    val habitColor = Color(habitColorL.toInt())
    val progress   = if (target > 0f) (value / target).coerceIn(0f, 1f) else if (value > 0f) 1f else 0f
    val done       = progress >= 1f || (trackingType == "BINARY" && value > 0f)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1D21))
            .cornerRadius(20.dp)
            .padding(10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // ── Header ─────────────────────────────────────────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = habitIcon, style = TextStyle(fontSize = 16.sp))
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text     = habitName,
                    style    = TextStyle(
                        color      = ColorProvider(Color.White),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }

            Spacer(GlanceModifier.height(6.dp))

            // ── Main value display ─────────────────────────────────────────────
            when (trackingType) {
                "BINARY" -> BinaryProgressView(streak = streak, done = done, color = habitColor)
                "DURATION" -> DurationProgressView(
                    elapsedSecs = value.toLong(),
                    targetSecs  = target.toLong(),
                    progress    = progress,
                    color       = habitColor,
                )
                else -> QuantProgressView(
                    value    = value,
                    target   = target,
                    unit     = unit,
                    progress = progress,
                    color    = habitColor,
                )
            }

            Spacer(GlanceModifier.defaultWeight())

            // ── Streak footer ──────────────────────────────────────────────────
            if (streak > 0 && trackingType != "BINARY") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "🔥 $streak day streak",
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFFFFB74D)),
                            fontSize = 9.sp,
                        ),
                    )
                }
            }
        }
    }
}

// ── Sub-views ─────────────────────────────────────────────────────────────────

@Composable
private fun BinaryProgressView(streak: Int, done: Boolean, color: Color) {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Large streak number
        Text(
            text  = if (streak > 0) "🔥" else if (done) "✓" else "○",
            style = TextStyle(fontSize = 28.sp),
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text  = if (streak > 0) "$streak day streak"
                    else if (done) "Done today!" else "Not done yet",
            style = TextStyle(
                color      = ColorProvider(if (done) color else Color(0xFF9E9E9E)),
                fontSize   = 10.sp,
                fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun DurationProgressView(
    elapsedSecs: Long,
    targetSecs: Long,
    progress: Float,
    color: Color,
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        // Time display
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text  = formatSecsShort(elapsedSecs),
                style = TextStyle(
                    color      = ColorProvider(color),
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (targetSecs > 0) {
                Text(
                    text  = " / ${formatSecsShort(targetSecs)}",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFF9E9E9E)),
                        fontSize = 10.sp,
                    ),
                )
            }
        }
        if (targetSecs > 0) {
            Spacer(GlanceModifier.height(6.dp))
            SegmentedBar(progress = progress, color = color)
        }
    }
}

@Composable
private fun QuantProgressView(
    value: Float,
    target: Float,
    unit: String,
    progress: Float,
    color: Color,
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text  = formatValue(value),
                style = TextStyle(
                    color      = ColorProvider(color),
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (target > 0f) {
                Text(
                    text  = " / ${formatValue(target)} $unit",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFF9E9E9E)),
                        fontSize = 10.sp,
                    ),
                )
            } else if (unit.isNotBlank()) {
                Text(
                    text  = " $unit",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFF9E9E9E)),
                        fontSize = 10.sp,
                    ),
                )
            }
        }
        if (target > 0f) {
            Spacer(GlanceModifier.height(6.dp))
            SegmentedBar(progress = progress, color = color)
        }
    }
}

/** 10-segment filled bar representing [progress] ∈ [0,1]. */
@Composable
private fun SegmentedBar(progress: Float, color: Color) {
    val filled = (progress * 10).toInt().coerceIn(0, 10)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        for (i in 0 until 10) {
            Box(
                modifier = GlanceModifier
                    .height(5.dp)
                    .defaultWeight()
                    .background(if (i < filled) color else Color(0xFF2D3339))
                    .cornerRadius(3.dp),
            ) {}
            if (i < 9) Spacer(GlanceModifier.width(2.dp))
        }
    }
}

// ── Formatting helpers ────────────────────────────────────────────────────────

private fun formatSecsShort(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) "%dh %02dm".format(h, m) else "%02d:%02d".format(m, s)
}

private fun formatValue(v: Float): String =
    if (v == v.toLong().toFloat()) v.toLong().toString() else "%.1f".format(v)
