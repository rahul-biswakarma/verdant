package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.GridEmptyDark
import com.verdant.core.designsystem.theme.GridEmptyLight
import com.verdant.core.designsystem.theme.MutedSage
import com.verdant.core.designsystem.theme.VerdantTheme
import com.verdant.core.designsystem.theme.gridCellColor
import com.verdant.core.model.DayCell
import java.time.LocalDate

/**
 * GitHub-style contribution grid.
 *
 * Layout: 7 rows (Mon–Sun) × [weeks] columns, horizontally scrollable.
 * The grid always ends on the current week, with the most recent column
 * scrolled into view on first composition.
 *
 * For habits using the default Verdant green the per-level palette is used;
 * for other colors the empty-cell base is lerped toward [habitColor].
 *
 * @param cells       All [DayCell] values to display (any date range).
 * @param habitColor  Base color for filled cells.
 * @param weeks       Number of week columns to render.
 * @param onCellClick Called when the user taps a cell.
 */
@Composable
fun HabitContributionGrid(
    cells: List<DayCell>,
    habitColor: Color,
    weeks: Int,
    onCellClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val today = remember { LocalDate.now() }
    val emptyColor = if (isDark) GridEmptyDark else GridEmptyLight

    // Build a weeks×7 grid anchored to today, filling gaps with empty cells.
    val weekColumns: List<List<DayCell>> = remember(cells, weeks, today) {
        val cellMap = cells.associateBy { it.date }

        // Find the Monday of the current week, then go back (weeks-1) weeks.
        val currentWeekMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val startMonday = currentWeekMonday.minusWeeks((weeks - 1).toLong())

        (0 until weeks).map { w ->
            (0 until 7).map { d ->
                val date = startMonday.plusDays((w * 7 + d).toLong())
                cellMap[date] ?: DayCell(
                    date = date,
                    intensity = 0f,
                    entryCount = 0,
                    completedCount = 0,
                )
            }
        }
    }

    val listState = rememberLazyListState()

    // Scroll to the newest (rightmost) column on first composition.
    LaunchedEffect(weekColumns.size) {
        if (weekColumns.isNotEmpty()) {
            listState.scrollToItem((weekColumns.size - 1).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(weekColumns) { weekCells ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                weekCells.forEach { cell ->
                    val cellColor = resolveCellColor(
                        intensity = cell.intensity,
                        habitColor = habitColor,
                        emptyColor = emptyColor,
                        isDark = isDark,
                    )
                    HabitGridCell(
                        intensity = cell.intensity,
                        color = cellColor,
                        size = 14.dp,
                        isToday = cell.date == today,
                        modifier = Modifier.clickable { onCellClick(cell.date) },
                    )
                }
            }
        }
    }
}

private fun resolveCellColor(
    intensity: Float,
    habitColor: Color,
    emptyColor: Color,
    isDark: Boolean,
): Color = when {
    intensity <= 0f -> emptyColor
    habitColor == MutedSage -> gridCellColor(intensity, isDark)
    else -> lerp(emptyColor, habitColor, intensity.coerceIn(0f, 1f))
}
private fun previewCells(weeks: Int): List<DayCell> {
    val today = LocalDate.now()
    return (0 until weeks * 7).map { i ->
        val date = today.minusDays((weeks * 7 - 1 - i).toLong())
        DayCell(
            date = date,
            intensity = when (i % 5) {
                0 -> 0f; 1 -> 0.25f; 2 -> 0.5f; 3 -> 0.75f; else -> 1f
            },
            entryCount = i % 5,
            completedCount = i % 5,
        )
    }
}

@Preview(name = "HabitContributionGrid – light", showBackground = true)
@Composable
private fun HabitContributionGridLightPreview() {
    VerdantTheme {
        HabitContributionGrid(
            cells = previewCells(16),
            habitColor = MutedSage,
            weeks = 16,
            onCellClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(
    name = "HabitContributionGrid – dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HabitContributionGridDarkPreview() {
    VerdantTheme(dynamicColor = false) {
        HabitContributionGrid(
            cells = previewCells(16),
            habitColor = MutedSage,
            weeks = 16,
            onCellClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(name = "HabitContributionGrid – custom color", showBackground = true)
@Composable
private fun HabitContributionGridCustomColorPreview() {
    VerdantTheme {
        HabitContributionGrid(
            cells = previewCells(16),
            habitColor = Color(0xFF2196F3),
            weeks = 16,
            onCellClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}
