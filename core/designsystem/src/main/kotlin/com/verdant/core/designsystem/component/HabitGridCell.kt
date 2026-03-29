package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.VerdantTheme
import com.verdant.core.designsystem.theme.gridCellColor

/**
 * Single circular cell in a contribution grid.
 *
 * @param intensity Completion intensity in [0, 1].
 * @param color     Pre-computed cell fill color.
 * @param size      Cell diameter.
 * @param isToday   When true, draws a highlight border around the cell.
 */
@Composable
fun HabitGridCell(
    intensity: Float,
    color: Color,
    size: Dp = 14.dp,
    isToday: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .then(
                if (isToday) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.tertiary,
                    RoundedCornerShape(4.dp),
                )
                else Modifier
            ),
    )
}
@Preview(name = "Grid cells – light", showBackground = true, backgroundColor = 0xFFF5F0EB)
@Composable
private fun HabitGridCellLightPreview() {
    VerdantTheme {
        val isDark = false
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
    backgroundColor = 0xFF141311,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HabitGridCellDarkPreview() {
    VerdantTheme {
        val isDark = true
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
