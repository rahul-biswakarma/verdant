package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdant.core.designsystem.theme.VerdantTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Data model ────────────────────────────────────────────────────────────────

/**
 * Input data for a single orbit in [OrbitalDecayChart].
 *
 * @param habitId           Unique habit identifier.
 * @param habitName         Display name shown as orbit label.
 * @param habitIcon         Emoji/icon shown at the orbiting dot.
 * @param color             Base color for this orbit.
 * @param daysSinceLast     Days elapsed since the last completion. Use -1 for "never done".
 * @param maxDaysBeforeUrgent Threshold after which the orbit is considered urgent (red).
 */
data class OrbitalHabitData(
    val habitId: String,
    val habitName: String,
    val habitIcon: String,
    val color: Color,
    val daysSinceLast: Int,
    val maxDaysBeforeUrgent: Int,
)

// ── Color helpers ─────────────────────────────────────────────────────────────

private val OrbitalGreen = Color(0xFF4CAF50)
private val OrbitalYellow = Color(0xFFFFC107)
private val OrbitalOrange = Color(0xFFFF9800)
private val OrbitalRed = Color(0xFFF44336)

/** Maps urgency [0,1] → green → yellow → orange → red. */
private fun urgencyColor(urgency: Float): Color = when {
    urgency < 0.33f -> lerp(OrbitalGreen, OrbitalYellow, urgency / 0.33f)
    urgency < 0.67f -> lerp(OrbitalYellow, OrbitalOrange, (urgency - 0.33f) / 0.34f)
    else -> lerp(OrbitalOrange, OrbitalRed, (urgency - 0.67f) / 0.33f)
}

// ── Main composable ───────────────────────────────────────────────────────────

/**
 * Canvas-based visualization for EVENT_DRIVEN habits.
 *
 * Each [OrbitalHabitData] renders as an orbit ring around a central node.
 * Rings drift outward as [OrbitalHabitData.daysSinceLast] increases and snap
 * back with a spring animation when the habit is completed (i.e. daysSinceLast resets).
 *
 * @param habits    List of event-driven habits to display.
 * @param modifier  Standard Compose modifier.
 */
@Composable
fun OrbitalDecayChart(
    habits: List<OrbitalHabitData>,
    modifier: Modifier = Modifier,
) {
    if (habits.isEmpty()) return

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Infinite rotation for orbiting dots
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbitAngle",
    )

    // Pulsing scale for urgent orbits (urgency > 0.66)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    // Animated radii: Animatable per habit keyed by habitId for spring snap-back
    val radiusAnimatables = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    habits.forEach { habit ->
        val target = if (habit.daysSinceLast < 0) 1f
        else (habit.daysSinceLast.toFloat() / habit.maxDaysBeforeUrgent.toFloat()).coerceIn(0f, 1f)
        if (habit.habitId !in radiusAnimatables) {
            radiusAnimatables[habit.habitId] = Animatable(target)
        }
        LaunchedEffect(habit.habitId, target) {
            radiusAnimatables[habit.habitId]?.animateTo(
                target,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            )
        }
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Radii bands: inner tight ring → outer decay ring
        val minRadius = with(density) { 36.dp.toPx() }
        val maxRadius = minOf(cx, cy) - with(density) { 32.dp.toPx() }
        val bandWidth = if (habits.size > 1) (maxRadius - minRadius) / habits.size else maxRadius - minRadius

        // Central node
        val centralRadius = with(density) { 14.dp.toPx() }
        drawCircle(color = surfaceVariant, radius = centralRadius + with(density) { 4.dp.toPx() }, center = Offset(cx, cy))
        drawCircle(color = onSurface.copy(alpha = 0.12f), radius = centralRadius, center = Offset(cx, cy))

        habits.forEachIndexed { index, habit ->
            val fraction = radiusAnimatables[habit.habitId]?.value ?: 0f
            val urgency = fraction
            val ringColor = urgencyColor(urgency)

            // Base ring sits at its band; decayed ring drifts outward within the band
            val baseRingRadius = minRadius + index * bandWidth
            val decayedRadius = baseRingRadius + fraction * bandWidth * 0.85f

            val isUrgent = urgency > 0.66f
            val effectiveRadius = if (isUrgent) decayedRadius * pulseScale else decayedRadius
            val orbitStroke = with(density) { 2.dp.toPx() }
            val dotRadius = with(density) { 6.dp.toPx() }

            // Ring track (subtle, always at decayed position)
            drawCircle(
                color = ringColor.copy(alpha = 0.15f),
                radius = effectiveRadius,
                center = Offset(cx, cy),
                style = Stroke(width = orbitStroke),
            )

            // Colored ring arc (full circle, intensity driven by urgency)
            drawCircle(
                color = ringColor.copy(alpha = 0.35f + urgency * 0.45f),
                radius = effectiveRadius,
                center = Offset(cx, cy),
                style = Stroke(width = orbitStroke * (1f + urgency)),
            )

            // Orbiting dot – each habit offset by index to spread them out
            val angleOffset = index * (2f * PI.toFloat() / habits.size)
            val dotAngle = orbitAngle + angleOffset
            val dotX = cx + effectiveRadius * cos(dotAngle)
            val dotY = cy + effectiveRadius * sin(dotAngle)

            // Dot halo
            drawCircle(
                color = ringColor.copy(alpha = 0.2f),
                radius = dotRadius * 1.8f,
                center = Offset(dotX, dotY),
            )
            // Dot fill
            drawCircle(
                color = ringColor,
                radius = dotRadius,
                center = Offset(dotX, dotY),
            )

            // "Time since" label on the ring, at the 3 o'clock position
            drawLabel(
                textMeasurer = textMeasurer,
                text = habit.daysSinceLast.toDaysAgoLabel(),
                cx = cx,
                cy = cy,
                radius = effectiveRadius,
                labelAngle = 0f, // 3 o'clock
                color = ringColor,
                density = density,
            )
        }
    }
}

// ── Single-orbit variant ───────────────────────────────────────────────────────

/**
 * Single-habit variant of [OrbitalDecayChart], suitable for the Habit Detail screen.
 * Shows one orbit with larger text labels and a bigger central icon.
 */
@Composable
fun SingleOrbitalDecayChart(
    habit: OrbitalHabitData,
    modifier: Modifier = Modifier,
) {
    OrbitalDecayChart(habits = listOf(habit), modifier = modifier)
}

// ── Drawing helpers ────────────────────────────────────────────────────────────

private fun DrawScope.drawLabel(
    textMeasurer: TextMeasurer,
    text: String,
    cx: Float,
    cy: Float,
    radius: Float,
    labelAngle: Float,
    color: Color,
    density: androidx.compose.ui.unit.Density,
) {
    val labelRadius = radius + with(density) { 10.dp.toPx() }
    val labelX = cx + labelRadius * cos(labelAngle)
    val labelY = cy + labelRadius * sin(labelAngle)

    val measured = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
        ),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            x = labelX - measured.size.width / 2f,
            y = labelY - measured.size.height / 2f,
        ),
    )
}

// ── Formatting helpers ────────────────────────────────────────────────────────

fun Int.toDaysAgoLabel(): String = when {
    this < 0 -> "never"
    this == 0 -> "today"
    this == 1 -> "yesterday"
    this < 7 -> "$this days ago"
    this < 14 -> "1 week ago"
    this < 21 -> "2 weeks ago"
    this < 28 -> "3 weeks ago"
    this < 60 -> "${this / 7} weeks ago"
    else -> "${this / 30}mo ago"
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "OrbitalDecayChart – light", showBackground = true)
@Composable
private fun OrbitalDecayChartLightPreview() {
    VerdantTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            OrbitalDecayChart(
                habits = previewHabits(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )
        }
    }
}

@Preview(
    name = "OrbitalDecayChart – dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun OrbitalDecayChartDarkPreview() {
    VerdantTheme(dynamicColor = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            OrbitalDecayChart(
                habits = previewHabits(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )
        }
    }
}

@Preview(name = "SingleOrbitalDecayChart – light", showBackground = true)
@Composable
private fun SingleOrbitalPreview() {
    VerdantTheme {
        SingleOrbitalDecayChart(
            habit = OrbitalHabitData(
                habitId = "1",
                habitName = "Call parents",
                habitIcon = "📞",
                color = Color(0xFF5A7A60),
                daysSinceLast = 8,
                maxDaysBeforeUrgent = 7,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
        )
    }
}

private fun previewHabits() = listOf(
    OrbitalHabitData("1", "Call parents", "📞", Color(0xFF5A7A60), daysSinceLast = 1, maxDaysBeforeUrgent = 7),
    OrbitalHabitData("2", "Deep cleaning", "🧹", Color(0xFF7B6B6B), daysSinceLast = 5, maxDaysBeforeUrgent = 14),
    OrbitalHabitData("3", "Car service", "🚗", Color(0xFFE8673C), daysSinceLast = 45, maxDaysBeforeUrgent = 90),
    OrbitalHabitData("4", "Dentist", "🦷", Color(0xFF1976D2), daysSinceLast = -1, maxDaysBeforeUrgent = 180),
)
