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

private val BgDark = Color(0xFF1C1C1E)
private val CardBorder = Color(0xFF2C2C2E)
private val TextMuted = Color(0xFF8E8E93)
private val GreenAccent = Color(0xFF34C759)
private val FlameOrange = Color(0xFFFF6B6B)

/**
 * Streak widget composable (2x2).
 *
 * Premium dark design with:
 *  - Fire icon + bold streak day count
 *  - Weekly Mon–Sun completion dots with checkmarks
 *  - Progress bar for today's top habit (measurable) or overall completion
 */
@androidx.compose.runtime.Composable
internal fun StreakContent() {
    val prefs = currentState<Preferences>()

    val topStreaksJson = prefs[WidgetPreferencesKeys.TOP_STREAKS_JSON] ?: "[]"
    val bestStreak    = prefs[WidgetPreferencesKeys.BEST_EVER_STREAK] ?: 0
    val weekDaysJson  = prefs[WidgetPreferencesKeys.WEEK_DAYS_JSON]   ?: "[]"
    val todayDone     = prefs[WidgetPreferencesKeys.TODAY_DONE]       ?: 0
    val todayTotal    = prefs[WidgetPreferencesKeys.TODAY_TOTAL]      ?: 0
    val habitName     = prefs[WidgetPreferencesKeys.HABIT_NAME]       ?: ""
    val unitLabel     = prefs[WidgetPreferencesKeys.UNIT_LABEL]       ?: ""
    val currentValue  = prefs[WidgetPreferencesKeys.CURRENT_VALUE]    ?: 0f
    val targetValue   = prefs[WidgetPreferencesKeys.TARGET_VALUE]     ?: 0f

    val streaks   = parseStreaksJson(topStreaksJson)
    val weekDays  = parseWeekDaysJson(weekDaysJson)
    val topStreak = streaks.firstOrNull()?.streak ?: 0

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgDark)
            .cornerRadius(20.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        // Inner card with subtle border effect
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(1.dp)
                .background(Color(0xFF1C1C1E))
                .cornerRadius(20.dp),
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                // ── Top: Fire icon + STREAK + Days ──
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Fire icon circle
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(Color(0xFF2C1215))
                            .cornerRadius(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\uD83D\uDD25",
                            style = TextStyle(fontSize = 16.sp),
                        )
                    }

                    Spacer(GlanceModifier.width(8.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "STREAK",
                            style = TextStyle(
                                color = ColorProvider(TextMuted),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "$topStreak DAYS",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }

                    // Best streak badge
                    if (bestStreak > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "BEST",
                                style = TextStyle(
                                    color = ColorProvider(TextMuted),
                                    fontSize = 8.sp,
                                ),
                            )
                            Text(
                                text = "$bestStreak",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFFFD60A)),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }

                Spacer(GlanceModifier.height(10.dp))

                // ── Divider ──
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardBorder),
                ) {}

                Spacer(GlanceModifier.height(10.dp))

                // ── Weekly dots: Mon–Sun ──
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for ((index, day) in weekDays.withIndex()) {
                        if (index > 0) Spacer(GlanceModifier.width(4.dp))
                        WeekDayDot(day = day)
                    }
                }

                Spacer(GlanceModifier.height(10.dp))

                // ── Bottom: Progress section ──
                val hasMeasurable = targetValue > 0f && unitLabel.isNotEmpty()
                if (hasMeasurable) {
                    // Measurable habit progress (like STEPS 6825/10,000)
                    MeasurableProgress(
                        label = unitLabel.uppercase(),
                        current = currentValue,
                        target = targetValue,
                    )
                } else {
                    // Overall today's completion progress
                    TodayProgress(done = todayDone, total = todayTotal)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WeekDayDot(day: WeekDay) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Circle
        val bgColor = when {
            day.done    -> GreenAccent
            day.partial -> Color(0xFF2D5A2E)
            day.today   -> Color.Transparent
            day.future  -> Color(0xFF3A3A3C)
            else        -> Color(0xFF3A3A3C)
        }
        val borderColor = if (day.today && !day.done) GreenAccent else bgColor

        Box(
            modifier = GlanceModifier
                .size(24.dp)
                .background(if (day.today && !day.done) Color.Transparent else bgColor)
                .cornerRadius(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (day.today && !day.done) {
                // Ring outline for today
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .background(Color(0xFF1A3D1B))
                        .cornerRadius(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(18.dp)
                            .background(BgDark)
                            .cornerRadius(9.dp),
                    ) {}
                }
            } else if (day.done) {
                Text(
                    text = "\u2713",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        Spacer(GlanceModifier.height(3.dp))

        // Day label
        Text(
            text = day.label,
            style = TextStyle(
                color = ColorProvider(
                    if (day.today) Color.White
                    else if (day.done) GreenAccent
                    else TextMuted
                ),
                fontSize = 8.sp,
                fontWeight = if (day.today) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
private fun MeasurableProgress(label: String, current: Float, target: Float) {
    val fraction = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
    val pct = (fraction * 100).toInt()
    val currentFmt = if (current >= 1000) "${(current / 1000).toInt()},${(current.toInt() % 1000).toString().padStart(3, '0')}"
                     else current.toInt().toString()
    val targetFmt = if (target >= 1000) "${(target / 1000).toInt()},${(target.toInt() % 1000).toString().padStart(3, '0')}"
                    else target.toInt().toString()

    Column {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(TextMuted),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(2.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = currentFmt,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "/$targetFmt",
                style = TextStyle(
                    color = ColorProvider(TextMuted),
                    fontSize = 12.sp,
                ),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "$pct%",
                style = TextStyle(
                    color = ColorProvider(TextMuted),
                    fontSize = 12.sp,
                ),
            )
        }

        Spacer(GlanceModifier.height(4.dp))

        // Progress bar
        ProgressBar(fraction = fraction, color = GreenAccent)
    }
}

@androidx.compose.runtime.Composable
private fun TodayProgress(done: Int, total: Int) {
    val fraction = if (total > 0) done.toFloat() / total else 0f
    val pct = (fraction * 100).toInt()

    Column {
        Text(
            text = "TODAY",
            style = TextStyle(
                color = ColorProvider(TextMuted),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(2.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "$done",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "/$total",
                style = TextStyle(
                    color = ColorProvider(TextMuted),
                    fontSize = 12.sp,
                ),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "$pct%",
                style = TextStyle(
                    color = ColorProvider(TextMuted),
                    fontSize = 12.sp,
                ),
            )
        }

        Spacer(GlanceModifier.height(4.dp))

        ProgressBar(fraction = fraction, color = GreenAccent)
    }
}

@androidx.compose.runtime.Composable
internal fun ProgressBar(fraction: Float, color: Color, height: Int = 6) {
    val barWidthPct = (fraction * 100).toInt().coerceIn(0, 100)
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height.dp)
            .background(Color(0xFF3A3A3C))
            .cornerRadius((height / 2).dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (barWidthPct > 0) {
            // Use a fixed-width approach: fill percentage of a max ~200dp bar
            val barDp = (barWidthPct * 2).coerceAtLeast(height)
            Box(
                modifier = GlanceModifier
                    .width(barDp.dp)
                    .height(height.dp)
                    .background(color)
                    .cornerRadius((height / 2).dp),
            ) {}
        }
    }
}

// ── Data models ──

internal data class WeekDay(
    val label: String,
    val done: Boolean,
    val partial: Boolean,
    val future: Boolean,
    val today: Boolean,
)

internal fun parseWeekDaysJson(json: String): List<WeekDay> {
    if (json.isBlank() || json == "[]") {
        return listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").map {
            WeekDay(label = it, done = false, partial = false, future = true, today = false)
        }
    }
    return runCatching {
        val arr = JSONArray(json)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(WeekDay(
                    label   = obj.getString("day"),
                    done    = obj.getBoolean("done"),
                    partial = obj.optBoolean("partial", false),
                    future  = obj.optBoolean("future", false),
                    today   = obj.optBoolean("today", false),
                ))
            }
        }
    }.getOrElse {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").map {
            WeekDay(label = it, done = false, partial = false, future = true, today = false)
        }
    }
}

internal data class StreakItem(
    val icon: String,
    val name: String,
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
                    icon   = obj.optString("icon", "\uD83C\uDF31"),
                    name   = obj.getString("name"),
                    colorL = obj.getLong("colorL"),
                    streak = obj.getInt("streak"),
                ))
            }
        }
    }.getOrElse { emptyList() }
}
