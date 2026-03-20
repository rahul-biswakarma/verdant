package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.GridEmptyDark
import com.verdant.core.designsystem.theme.GridEmptyLight
import com.verdant.core.designsystem.theme.MutedSage
import com.verdant.core.designsystem.theme.VerdantTheme
import com.verdant.core.designsystem.theme.gridCellColor

private val LegendLevels = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

/**
 * Five-circle color legend mirroring the contribution grid intensity scale.
 *
 * When [color] matches the default sage, the circles align exactly
 * with the grid palette; for other colors they lerp from the
 * empty-cell color up to the full [color].
 */
@Composable
fun IntensityLegend(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val emptyColor = if (isDark) GridEmptyDark else GridEmptyLight

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LegendLevels.forEach { intensity ->
            val cellColor = if (color == MutedSage) {
                gridCellColor(intensity, isDark)
            } else {
                if (intensity <= 0f) emptyColor else lerp(emptyColor, color, intensity)
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(cellColor),
            )
        }

        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "IntensityLegend – light (sage)", showBackground = true)
@Composable
private fun IntensityLegendSageLightPreview() {
    VerdantTheme {
        IntensityLegend(color = MutedSage, modifier = Modifier.padding(12.dp))
    }
}

@Preview(
    name = "IntensityLegend – dark (sage)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun IntensityLegendSageDarkPreview() {
    VerdantTheme {
        IntensityLegend(color = MutedSage, modifier = Modifier.padding(12.dp))
    }
}

@Preview(name = "IntensityLegend – light (blue)", showBackground = true)
@Composable
private fun IntensityLegendBlueLightPreview() {
    VerdantTheme {
        IntensityLegend(color = Color(0xFF2196F3), modifier = Modifier.padding(12.dp))
    }
}
