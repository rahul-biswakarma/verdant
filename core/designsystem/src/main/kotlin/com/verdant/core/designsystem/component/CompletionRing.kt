package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.MutedSage
import com.verdant.core.designsystem.theme.VerdantTheme

/**
 * Circular progress ring drawn with [Canvas].
 *
 * @param progress Value in [0, 1].
 * @param color    Arc fill color.
 * @param size     Outer diameter of the ring.
 */
@Composable
fun CompletionRing(
    progress: Float,
    color: Color,
    size: Dp = 48.dp,
    strokeWidth: Dp = 6.dp,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val stroke = with(LocalDensity.current) { strokeWidth.toPx() }
    val clamped = progress.coerceIn(0f, 1f)

    Canvas(modifier = modifier.size(size)) {
        // Background track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        // Progress arc
        if (clamped > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "CompletionRing – light", showBackground = true)
@Composable
private fun CompletionRingLightPreview() {
    VerdantTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            CompletionRing(progress = 0f, color = MutedSage)
            CompletionRing(progress = 0.4f, color = MutedSage)
            CompletionRing(progress = 1f, color = MutedSage)
        }
    }
}

@Preview(
    name = "CompletionRing – dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CompletionRingDarkPreview() {
    VerdantTheme(dynamicColor = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            CompletionRing(progress = 0f, color = MutedSage)
            CompletionRing(progress = 0.65f, color = MutedSage)
            CompletionRing(progress = 1f, color = MutedSage)
        }
    }
}
