package com.verdant.feature.analytics.tab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdant.core.designsystem.theme.WarmCharcoal
import com.verdant.feature.analytics.TrendSeries
import com.verdant.feature.analytics.TrendsState
import kotlin.math.roundToInt

@Composable
fun TrendsTab(
    state: TrendsState,
    onSeriesSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.series.isEmpty()) {
        EmptyAnalyticsState(
            message = "Track habits for at least a week to see trends",
            modifier = modifier,
        )
        return
    }

    val selectedSeries = state.series.firstOrNull { s ->
        if (state.selectedSeriesKey == "overall") s.label == "Overall"
        else s.label == state.selectedSeriesKey
    } ?: state.series.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text  = "Show",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.series.forEach { series ->
                    val key = if (series.label == "Overall") "overall" else series.label
                    FilterChip(
                        selected = state.selectedSeriesKey == key,
                        onClick  = { onSeriesSelected(key) },
                        label    = { Text(series.label, maxLines = 1) },
                    )
                }
            }
        }
        SectionCard(title = "12-Week Completion Rate") {
            CompletionLineChart(
                series     = selectedSeries,
                weekLabels = state.weekLabels,
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
        val values = selectedSeries.values
        val avg    = if (values.isEmpty()) 0f else values.average().toFloat()
        val max    = values.maxOrNull() ?: 0f
        val trend  = if (values.size >= 4) {
            val recent  = values.takeLast(4).average()
            val earlier = values.take(4).average()
            when {
                recent > earlier + 0.05 -> "↑ Improving"
                recent < earlier - 0.05 -> "↓ Declining"
                else -> "→ Stable"
            }
        } else "—"

        SectionCard(title = "Summary — ${selectedSeries.label}") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                TrendStatItem("Average",   "${(avg * 100).roundToInt()}%")
                TrendStatItem("Best week", "${(max * 100).roundToInt()}%")
                TrendStatItem("Trend",      trend)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CompletionLineChart(
    series: TrendSeries,
    weekLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    val lineColor  = if (series.color != 0L) Color(series.color) else WarmCharcoal
    val gridColor  = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier.padding(bottom = 24.dp, start = 36.dp)) {
        val width  = size.width
        val height = size.height
        val values = series.values

        if (values.isEmpty()) return@Canvas

        val n = values.size
        val xStep = if (n > 1) width / (n - 1).toFloat() else width

        // Draw horizontal grid lines at 0%, 25%, 50%, 75%, 100%
        val gridLines = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        gridLines.forEach { level ->
            val y = height - level * height
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end   = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
            )
            // Y-axis labels
            val labelText = "${(level * 100).toInt()}%"
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                -32.dp.toPx(),
                y + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(
                        (labelColor.alpha * 255).toInt(),
                        (labelColor.red   * 255).toInt(),
                        (labelColor.green * 255).toInt(),
                        (labelColor.blue  * 255).toInt(),
                    )
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                },
            )
        }

        // Build line path
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * xStep
            val y = height - v.coerceIn(0f, 1f) * height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Draw filled area
        val fillPath = Path().apply {
            addPath(path)
            lineTo((n - 1) * xStep, height)
            lineTo(0f, height)
            close()
        }
        drawPath(fillPath, color = lineColor.copy(alpha = 0.12f))

        // Draw line
        drawPath(
            path   = path,
            color  = lineColor,
            style  = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Draw data points
        values.forEachIndexed { i, v ->
            val x = i * xStep
            val y = height - v.coerceIn(0f, 1f) * height
            drawCircle(color = lineColor,         radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color.White,       radius = 2.dp.toPx(), center = Offset(x, y))
        }

        // X-axis labels (every other label to avoid crowding)
        weekLabels.forEachIndexed { i, label ->
            if (i % 2 == 0 || n <= 6) {
                val x = i * xStep
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    height + 20.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (labelColor.alpha * 255).toInt(),
                            (labelColor.red   * 255).toInt(),
                            (labelColor.green * 255).toInt(),
                            (labelColor.blue  * 255).toInt(),
                        )
                        textSize = 8.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    },
                )
            }
        }
    }
}

@Composable
private fun TrendStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
