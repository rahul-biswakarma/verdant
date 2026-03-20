package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.VerdantGreen40
import com.verdant.core.designsystem.theme.VerdantTheme
import com.verdant.core.designsystem.theme.gridCellColor

private val CellShape = RoundedCornerShape(2.dp)

/**
 * Single square cell in a contribution grid.
 *
 * @param intensity Completion intensity in [0, 1].
 * @param color     Pre-computed cell fill color.
 * @param size      Cell side length.
 * @param isToday   When true, draws a highlight border around the cell.
 */
@Composable
fun HabitGridCell(
    intensity: Float,
    color: Color,
    size: Dp = 12.dp,
    isToday: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CellShape)
            .background(color)
            .then(
                if (isToday) Modifier.border(1.dp, Color.White.copy(alpha = 0.8f), CellShape)
                else Modifier
            ),
    )
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "Grid cells – light", showBackground = true, backgroundColor = 0xFFF8FAF2)
@Composable
private fun HabitGridCellLightPreview() {
    VerdantTheme {
        val isDark = false
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        ) {
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEachIndexed { i, intensity ->
                HabitGridCell(
                    intensity = intensity,
                    color = gridCellColor(intensity, isDark),
                    isToday = i == 4,
                )
            }
        }
    }
}

@Preview(
    name = "Grid cells – dark",
    showBackground = true,
    backgroundColor = 0xFF1A1C19,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HabitGridCellDarkPreview() {
    VerdantTheme(dynamicColor = false) {
        val isDark = true
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        ) {
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEachIndexed { i, intensity ->
                HabitGridCell(
                    intensity = intensity,
                    color = gridCellColor(intensity, isDark),
                    isToday = i == 4,
                )
            }
        }
    }
}
