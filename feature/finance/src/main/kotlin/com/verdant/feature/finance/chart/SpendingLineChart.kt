package com.verdant.feature.finance.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdant.core.designsystem.theme.BurntOrange
import java.text.NumberFormat
import java.util.Locale

private val compactCurrencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

@Composable
fun SpendingLineChart(
    monthlyTotals: Map<String, Double>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(monthlyTotals) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    // Sort entries chronologically
    val entries = monthlyTotals.entries
        .sortedBy { it.key }
        .map { (month, amount) -> month to amount }

    if (entries.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        val leftPadding = 56.dp.toPx()
        val rightPadding = 16.dp.toPx()
        val topPadding = 16.dp.toPx()
        val bottomPadding = 32.dp.toPx()

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        val maxAmount = entries.maxOf { it.second }.coerceAtLeast(1.0)

        // Horizontal grid lines
        val gridSteps = listOf(0.25f, 0.5f, 0.75f, 1f)
        gridSteps.forEach { step ->
            val y = topPadding + chartHeight * (1f - step)
            drawLine(
                color = surfaceVariant,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1.dp.toPx(),
            )
            // Y-axis labels
            val label = compactCurrencyFormat.format(maxAmount * step)
            val textResult = textMeasurer.measure(label, labelStyle)
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = leftPadding - textResult.size.width - 8.dp.toPx(),
                    y = y - textResult.size.height / 2f,
                ),
            )
        }

        // Data points
        val points = entries.mapIndexed { index, (_, amount) ->
            val x = leftPadding + if (entries.size > 1) {
                chartWidth * index / (entries.size - 1)
            } else {
                chartWidth / 2f
            }
            val normalizedY = (amount / maxAmount).toFloat() * animProgress.value
            val y = topPadding + chartHeight * (1f - normalizedY)
            Offset(x, y)
        }

        // Filled area under curve
        if (points.size >= 2) {
            val areaPath = Path().apply {
                moveTo(points.first().x, topPadding + chartHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, topPadding + chartHeight)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BurntOrange.copy(alpha = 0.2f),
                        BurntOrange.copy(alpha = 0.02f),
                    ),
                    startY = topPadding,
                    endY = topPadding + chartHeight,
                ),
            )

            // Line
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = BurntOrange,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Dots
        points.forEach { point ->
            drawCircle(
                color = BurntOrange,
                radius = 5.dp.toPx(),
                center = point,
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = point,
            )
        }

        // X-axis month labels
        entries.forEachIndexed { index, (month, _) ->
            val x = points[index].x
            // Show short month label (e.g. "Mar" from "2026-03")
            val label = month.takeLast(2).let { mm ->
                val monthNum = mm.toIntOrNull() ?: return@forEachIndexed
                java.time.Month.of(monthNum).name.take(3)
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
            }
            val textResult = textMeasurer.measure(label, labelStyle)
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = x - textResult.size.width / 2f,
                    y = topPadding + chartHeight + 8.dp.toPx(),
                ),
            )
        }
    }
}
