package com.verdant.core.designsystem.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.GridEmptyDark
import com.verdant.core.designsystem.theme.GridEmptyLight
import com.verdant.core.designsystem.theme.WarmCharcoal
import com.verdant.core.designsystem.theme.gridCellColor
import com.verdant.core.model.DayCell
import java.time.LocalDate

/**
 * Compact, non-scrollable contribution grid for embedding in list cards.
 *
 * Displays [weeks] columns x 7 rows of 8dp cells, anchored to the current week.
 */
@Composable
fun MiniContributionGrid(
    cells: List<DayCell>,
    habitColor: Color,
    modifier: Modifier = Modifier,
    weeks: Int = 4,
) {
    val isDark = isSystemInDarkTheme()
    val today = remember { LocalDate.now() }
    val emptyColor = if (isDark) GridEmptyDark else GridEmptyLight

    val weekColumns = remember(cells, weeks, today) {
        val cellMap = cells.associateBy { it.date }
        val currentWeekMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val startMonday = currentWeekMonday.minusWeeks((weeks - 1).toLong())

        (0 until weeks).map { w ->
            (0 until 7).map { d ->
                val date = startMonday.plusDays((w * 7 + d).toLong())
                cellMap[date] ?: DayCell(date = date, intensity = 0f, entryCount = 0, completedCount = 0)
            }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        weekColumns.forEach { weekCells ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                weekCells.forEach { cell ->
                    val cellColor = when {
                        cell.intensity <= 0f -> emptyColor
                        habitColor == WarmCharcoal -> gridCellColor(cell.intensity, isDark)
                        else -> lerp(emptyColor, habitColor, cell.intensity.coerceIn(0f, 1f))
                    }
                    HabitGridCell(
                        intensity = cell.intensity,
                        color = cellColor,
                        size = 8.dp,
                        isToday = cell.date == today,
                    )
                }
            }
        }
    }
}
