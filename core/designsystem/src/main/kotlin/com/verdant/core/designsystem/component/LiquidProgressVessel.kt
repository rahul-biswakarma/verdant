package com.verdant.core.designsystem.component

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdant.core.designsystem.theme.VerdantTheme
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val ColorCool = Color(0xFF1E88E5)  // deep blue — empty vessel
private val ColorWarm = Color(0xFFFFB300)  // amber/gold — full vessel

/**
 * Liquid-physics progress vessel for QUANTITATIVE habits.
 *
 * The vessel fills with simulated liquid as [currentValue] approaches [targetValue].
 * The liquid surface responds to device tilt via the gyroscope, producing a sloshing
 * sine-wave surface. Color shifts from cool blue → [color] → warm gold as fill rises.
 *
 * @param currentValue  The logged value so far today (or period).
 * @param targetValue   The goal/target for this habit.
 * @param color         The habit's brand color — used at 50 % fill.
 * @param unit          Optional unit string displayed in the text overlay.
 * @param mini          When true, renders a compact version without text (for cards).
 * @param modifier      Modifier applied to the [Canvas].
 */
@Composable
fun LiquidProgressVessel(
    currentValue: Double,
    targetValue: Double,
    color: Color,
    modifier: Modifier = Modifier,
    unit: String = "",
    mini: Boolean = false,
) {
    val rawFill = if (targetValue > 0.0) (currentValue / targetValue).toFloat().coerceIn(0f, 1f) else 0f

    // ── Animate fill level ────────────────────────────────────────────────────
    val animatedFill = remember { Animatable(0f) }
    LaunchedEffect(rawFill) {
        animatedFill.animateTo(rawFill, animationSpec = tween(durationMillis = 900))
    }

    // ── Gyroscope ─────────────────────────────────────────────────────────────
    var gyroY by remember { mutableFloatStateOf(0f) }  // rad/s around Y-axis (left-right tilt)
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                gyroY = event.values[1]
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (gyro != null) sm.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    // ── Wave physics state ────────────────────────────────────────────────────
    var wavePhase by remember { mutableFloatStateOf(0f) }         // auto-oscillation offset
    var sloshOffset by remember { mutableFloatStateOf(0f) }       // gyro-driven lateral shift
    var sloshVelocity by remember { mutableFloatStateOf(0f) }     // slosh velocity (px/s)

    LaunchedEffect(Unit) {
        var lastMs = 0L
        while (isActive) {
            val frameMs = withFrameMillisCompat()
            val dt = if (lastMs == 0L) 0f else (frameMs - lastMs) / 1000f
            lastMs = frameMs

            // Gentle autonomous oscillation
            wavePhase += dt * 1.6f

            // Gyroscope → slosh: tilt multiplied to feel natural on screen
            sloshVelocity += gyroY * 120f * dt
            sloshVelocity *= 0.90f          // velocity damping
            sloshOffset += sloshVelocity * dt
            sloshOffset *= 0.97f            // slowly drift back to centre
        }
    }

    // ── Derived color based on fill ───────────────────────────────────────────
    val liquidColor = liquidColorAtFill(rawFill, color)

    // ── Text measurement (full mode only) ─────────────────────────────────────
    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerPx = with(density) { 20.dp.toPx() }
        val borderPx = with(density) { if (mini) 2.dp.toPx() else 3.dp.toPx() }
        val baseAmplitudePx = with(density) { if (mini) 2.5.dp.toPx() else 5.dp.toPx() }
        val waveAmplitudePx = (baseAmplitudePx + abs(sloshVelocity) * 0.18f).coerceAtMost(baseAmplitudePx * 2.8f)

        // ── Vessel clip path ──────────────────────────────────────────────────
        val vesselPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = borderPx / 2f,
                    top = borderPx / 2f,
                    right = w - borderPx / 2f,
                    bottom = h - borderPx / 2f,
                    cornerRadius = CornerRadius(cornerPx),
                ),
            )
        }

        val fillLevel = animatedFill.value
        val fillHeightPx = h * fillLevel
        val fillTopY = h - fillHeightPx

        // ── Liquid fill ───────────────────────────────────────────────────────
        clipPath(vesselPath) {
            drawLiquid(
                fillTopY = fillTopY,
                waveAmplitudePx = waveAmplitudePx,
                wavePhase = wavePhase,
                sloshOffset = sloshOffset,
                liquidColor = liquidColor,
                width = w,
                height = h,
            )
        }

        // ── Vessel border ─────────────────────────────────────────────────────
        drawPath(
            path = vesselPath,
            color = liquidColor.copy(alpha = 0.55f),
            style = Stroke(width = borderPx, cap = StrokeCap.Round),
        )

        // ── Text overlay (full mode only) ─────────────────────────────────────
        if (!mini) {
            val pct = "${(fillLevel * 100).toInt()}%"
            val valueLine = buildString {
                append(currentValue.fmtShort())
                append(" / ")
                append(targetValue.fmtShort())
                if (unit.isNotBlank()) append(" $unit")
            }

            val textColor = if (fillLevel > 0.52f) Color.White else onSurface
            val subColor = if (fillLevel > 0.58f) Color.White.copy(alpha = 0.82f) else onSurfaceVariant

            val pctLayout = textMeasurer.measure(
                pct,
                TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = textColor),
            )
            val subLayout = textMeasurer.measure(
                valueLine,
                TextStyle(fontSize = 12.sp, color = subColor),
            )

            val centerY = h / 2f
            val totalTextH = pctLayout.size.height + subLayout.size.height + with(density) { 2.dp.toPx() }
            val textStartY = centerY - totalTextH / 2f

            drawText(pctLayout, topLeft = Offset((w - pctLayout.size.width) / 2f, textStartY))
            drawText(
                subLayout,
                topLeft = Offset(
                    (w - subLayout.size.width) / 2f,
                    textStartY + pctLayout.size.height + with(density) { 2.dp.toPx() },
                ),
            )
        }
    }
}

// ── Internal drawing ──────────────────────────────────────────────────────────

private fun DrawScope.drawLiquid(
    fillTopY: Float,
    waveAmplitudePx: Float,
    wavePhase: Float,
    sloshOffset: Float,
    liquidColor: Color,
    width: Float,
    height: Float,
) {
    if (fillTopY >= height) return  // nothing to fill

    // Wave frequency: roughly 1.4 complete waves across the vessel width
    val freq = 2.2f * PI.toFloat() / width.coerceAtLeast(1f)
    val phaseShift = wavePhase + sloshOffset / 55f

    // Build liquid fill path ─────────────────────────────────────────────────
    val liquidPath = Path()
    liquidPath.moveTo(0f, height)
    liquidPath.lineTo(0f, fillTopY + sin(phaseShift) * waveAmplitudePx)

    val stepPx = 3f
    var x = stepPx
    while (x <= width) {
        liquidPath.lineTo(x, fillTopY + sin(freq * x + phaseShift) * waveAmplitudePx)
        x += stepPx
    }
    liquidPath.lineTo(width, height)
    liquidPath.close()

    // Gradient: lighter at surface, full colour lower down ───────────────────
    drawPath(
        path = liquidPath,
        brush = Brush.verticalGradient(
            colors = listOf(liquidColor.copy(alpha = 0.72f), liquidColor),
            startY = fillTopY - waveAmplitudePx,
            endY = height,
        ),
    )

    // Wave surface highlight ─────────────────────────────────────────────────
    val highlightPath = Path()
    var first = true
    x = 0f
    while (x <= width) {
        val wy = fillTopY + sin(freq * x + phaseShift) * waveAmplitudePx - waveAmplitudePx * 0.4f
        if (first) { highlightPath.moveTo(x, wy); first = false } else highlightPath.lineTo(x, wy)
        x += stepPx
    }
    drawPath(
        path = highlightPath,
        color = Color.White.copy(alpha = 0.30f),
        style = Stroke(width = 1.8.dp.toPx()),
    )
}

// ── Color interpolation ───────────────────────────────────────────────────────

/**
 * Interpolates liquid colour across three stops:
 *   0 %  → cool blue
 *   50 % → habit [color]
 *   100% → warm amber/gold
 */
private fun liquidColorAtFill(fill: Float, color: Color): Color = when {
    fill <= 0.5f -> lerp(ColorCool, color, fill * 2f)
    else -> lerp(color, ColorWarm, (fill - 0.5f) * 2f)
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun Double.fmtShort(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.1f".format(this)

/**
 * Multiplatform-friendly withFrameMillis that returns the frame timestamp.
 * Uses [androidx.compose.runtime.withFrameMillis] internally.
 */
private suspend fun withFrameMillisCompat(): Long {
    var ts = 0L
    androidx.compose.runtime.withFrameMillis { ts = it }
    return ts
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun LiquidVesselPreview() {
    VerdantTheme {
        Row(
            Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            LiquidProgressVessel(
                currentValue = 0.0,
                targetValue = 10.0,
                color = Color(0xFF43A047),
                unit = "km",
                modifier = Modifier.size(120.dp, 160.dp),
            )
            LiquidProgressVessel(
                currentValue = 4.5,
                targetValue = 10.0,
                color = Color(0xFF43A047),
                unit = "km",
                modifier = Modifier.size(120.dp, 160.dp),
            )
            LiquidProgressVessel(
                currentValue = 10.0,
                targetValue = 10.0,
                color = Color(0xFF43A047),
                unit = "km",
                modifier = Modifier.size(120.dp, 160.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun LiquidVesselMiniPreview() {
    VerdantTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LiquidProgressVessel(
                currentValue = 1500.0,
                targetValue = 2500.0,
                color = Color(0xFF1565C0),
                unit = "ml",
                mini = true,
                modifier = Modifier.size(56.dp, 44.dp),
            )
        }
    }
}
