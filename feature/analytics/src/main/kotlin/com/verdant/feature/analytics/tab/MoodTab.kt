package com.verdant.feature.analytics.tab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.component.MoodLegend
import com.verdant.core.designsystem.component.YearInPixelsGrid
import com.verdant.core.designsystem.component.moodScoreToColor
import com.verdant.core.designsystem.component.moodScoreToEmoji
import com.verdant.core.designsystem.theme.MutedSage
import com.verdant.feature.analytics.MoodState
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun MoodTab(
    state: MoodState,
    modifier: Modifier = Modifier,
) {
    if (state.entries.isEmpty()) {
        EmptyAnalyticsState(
            message = "Add an Emotional habit and start logging your mood to see the Year in Pixels",
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Year in Pixels ─────────────────────────────────────────────────
        SectionCard(title = "${state.year} — Year in Pixels") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                YearInPixelsGrid(
                    entries = state.entries,
                    year = state.year,
                    onDayClick = { /* detail handled in HabitDetail */ },
                    overlayDates = state.completionOverlay,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MoodLegend()
                    if (state.completionOverlay.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.85f)),
                            )
                            Text(
                                "= habit done",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // ── Mood stats ─────────────────────────────────────────────────────
        SectionCard(title = "Statistics") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                MoodStatItem(
                    label = "Average\nMood",
                    value = "%.1f".format(state.averageMood),
                    emoji = moodScoreToEmoji(state.averageMood.roundToInt().coerceIn(1, 5)),
                )
                MoodStatItem(
                    label = "Days\nLogged",
                    value = "${state.daysLogged}",
                    emoji = "📅",
                )
                val best = state.entries.maxByOrNull { it.moodScore }
                MoodStatItem(
                    label = "Best Day",
                    value = best?.date?.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")) ?: "–",
                    emoji = "😄",
                )
                val worst = state.entries.minByOrNull { it.moodScore }
                MoodStatItem(
                    label = "Rough Day",
                    value = worst?.date?.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")) ?: "–",
                    emoji = "😢",
                )
            }
        }

        // ── Mood distribution ──────────────────────────────────────────────
        SectionCard(title = "Mood Distribution") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (5 downTo 1).forEach { score ->
                    val count = state.entries.count { it.moodScore == score }
                    val fraction = if (state.entries.isEmpty()) 0f else count.toFloat() / state.entries.size
                    MoodDistributionBar(
                        score = score,
                        count = count,
                        fraction = fraction,
                    )
                }
            }
        }

        // ── Weekly mood trend ──────────────────────────────────────────────
        if (state.weeklyMoodTrend.any { it > 0f }) {
            SectionCard(title = "12-Week Trend") {
                MoodTrendChart(
                    values = state.weeklyMoodTrend,
                    labels = state.weekLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Mood stat item ─────────────────────────────────────────────────────────────

@Composable
private fun MoodStatItem(label: String, value: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ── Mood distribution bar ──────────────────────────────────────────────────────

@Composable
private fun MoodDistributionBar(score: Int, count: Int, fraction: Float) {
    val barColor = moodScoreToColor(score)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            moodScoreToEmoji(score),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(24.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor),
            )
        }
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
    }
}

// ── Mood trend chart (simple line) ────────────────────────────────────────────

@Composable
private fun MoodTrendChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.padding(top = 8.dp, bottom = 20.dp)) {
        if (values.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val minVal = 0f
        val maxVal = 5f
        val stepX = if (values.size > 1) w / (values.size - 1) else w

        // Grid lines at 1, 2, 3, 4, 5
        (1..5).forEach { y ->
            val yPos = h - (y - minVal) / (maxVal - minVal) * h
            drawLine(
                color = surfaceVariant,
                start = Offset(0f, yPos),
                end = Offset(w, yPos),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Path
        val pointsWithValues = values.mapIndexedNotNull { idx, v ->
            if (v > 0f) idx to v else null
        }

        if (pointsWithValues.size >= 2) {
            val path = Path()
            var first = true
            pointsWithValues.forEach { (idx, v) ->
                val x = idx * stepX
                val y = h - (v - minVal) / (maxVal - minVal) * h
                if (first) { path.moveTo(x, y); first = false }
                else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = MutedSage,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Dots
        pointsWithValues.forEach { (idx, v) ->
            val x = idx * stepX
            val y = h - (v - minVal) / (maxVal - minVal) * h
            val dotColor = lerp(
                moodScoreToColor(v.coerceIn(1f, 5f).toInt()),
                MutedSage,
                0.3f,
            )
            drawCircle(color = dotColor, radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(
                color = onSurface,
                radius = 4.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(1.dp.toPx()),
            )
        }
    }
}
