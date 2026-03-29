package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.WarmCharcoal
import com.verdant.core.designsystem.theme.VerdantTheme
import kotlin.math.sin

/**
 * A jar/bottle shaped container with animated liquid fill.
 *
 * @param progress Fill level in [0, 1].
 * @param color Liquid color.
 * @param size Outer dimensions of the jar.
 * @param animate Whether to animate the wave on the liquid surface.
 */
@Composable
fun PhysicsJar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animate: Boolean = true,
) {
    val clamped = progress.coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "jar_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_phase",
    )

    val phase = if (animate) wavePhase else 0f

    Canvas(modifier = modifier.size(size)) {
        drawJar(clamped, color, phase)
    }
}

private fun DrawScope.drawJar(progress: Float, color: Color, wavePhase: Float) {
    val w = size.width
    val h = size.height
    val cornerR = w * 0.2f
    val strokeW = w * 0.06f

    // Jar outline path (rounded rectangle)
    val jarRect = Rect(
        left = strokeW,
        top = strokeW,
        right = w - strokeW,
        bottom = h - strokeW,
    )
    val jarPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = jarRect,
                cornerRadius = CornerRadius(cornerR, cornerR),
            )
        )
    }

    // Draw jar background (empty)
    drawRoundRect(
        color = color.copy(alpha = 0.1f),
        topLeft = Offset(strokeW, strokeW),
        size = Size(w - 2 * strokeW, h - 2 * strokeW),
        cornerRadius = CornerRadius(cornerR, cornerR),
    )

    // Draw liquid clipped to jar shape
    if (progress > 0f) {
        clipPath(jarPath) {
            val liquidTop = jarRect.bottom - (jarRect.height * progress)
            val waveAmplitude = jarRect.width * 0.04f

            // Liquid body with wave surface
            val liquidPath = Path().apply {
                moveTo(jarRect.left, jarRect.bottom)
                lineTo(jarRect.left, liquidTop)

                // Wave across the top
                val steps = 40
                val stepWidth = jarRect.width / steps
                for (i in 0..steps) {
                    val x = jarRect.left + i * stepWidth
                    val normalizedX = i.toFloat() / steps
                    val y = liquidTop + sin(normalizedX * 4f * Math.PI.toFloat() + wavePhase) * waveAmplitude
                    lineTo(x, y)
                }

                lineTo(jarRect.right, jarRect.bottom)
                close()
            }

            drawPath(liquidPath, color.copy(alpha = 0.7f))

            // Lighter overlay for depth effect
            val overlayPath = Path().apply {
                val midX = jarRect.left + jarRect.width * 0.3f
                val overlayWidth = jarRect.width * 0.15f
                moveTo(midX, liquidTop)
                lineTo(midX, jarRect.bottom)
                lineTo(midX + overlayWidth, jarRect.bottom)
                lineTo(midX + overlayWidth, liquidTop)
                close()
            }
            drawPath(overlayPath, Color.White.copy(alpha = 0.15f))
        }
    }

    // Draw jar outline
    drawRoundRect(
        color = color.copy(alpha = 0.4f),
        topLeft = Offset(strokeW, strokeW),
        size = Size(w - 2 * strokeW, h - 2 * strokeW),
        cornerRadius = CornerRadius(cornerR, cornerR),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW),
    )
}

@Preview(name = "PhysicsJar – light", showBackground = true)
@Composable
private fun PhysicsJarLightPreview() {
    VerdantTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            PhysicsJar(progress = 0f, color = WarmCharcoal, animate = false)
            PhysicsJar(progress = 0.4f, color = WarmCharcoal, animate = false)
            PhysicsJar(progress = 0.75f, color = WarmCharcoal, animate = false)
            PhysicsJar(progress = 1f, color = WarmCharcoal, animate = false)
        }
    }
}

@Preview(
    name = "PhysicsJar – dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PhysicsJarDarkPreview() {
    VerdantTheme(dynamicColor = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            PhysicsJar(progress = 0.3f, color = WarmCharcoal, animate = false)
            PhysicsJar(progress = 0.65f, color = WarmCharcoal, animate = false)
        }
    }
}
