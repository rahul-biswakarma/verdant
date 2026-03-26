package com.verdant.feature.habits.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronLeft
import compose.icons.tablericons.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.verdant.core.model.DayCell
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Composable
fun CalendarTab(
    month: LocalDate,
    cells: List<DayCell>,
    habitColor: Color,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellMap = cells.associateBy { it.date }
    val today = LocalDate.now()
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfWeek = month.withDayOfMonth(1).dayOfWeek.value
    val totalCells = firstDayOfWeek - 1 + daysInMonth
    val rows = ceil(totalCells / 7.0).toInt()

    Column(modifier = modifier) {
        // Month header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(TablerIcons.ChevronLeft, "Previous month")
            }
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val isCurrentMonth = !month.plusMonths(1).isAfter(today.withDayOfMonth(1))
            IconButton(onClick = onNextMonth, enabled = isCurrentMonth) {
                Icon(
                    TablerIcons.ChevronRight,
                    "Next month",
                    tint = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Day grid
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - (firstDayOfWeek - 2)
                    if (dayOfMonth < 1 || dayOfMonth > daysInMonth) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = month.withDayOfMonth(dayOfMonth)
                        val cell = cellMap[date]
                        val isFuture = date.isAfter(today)
                        val isToday = date == today
                        val intensity = cell?.intensity ?: 0f
                        val bgColor = when {
                            isFuture -> Color.Transparent
                            intensity <= 0f -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else -> habitColor.copy(alpha = 0.15f + intensity * 0.7f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(
                                    if (isToday) Modifier.border(2.dp, habitColor, CircleShape)
                                    else Modifier,
                                )
                                .clickable(enabled = !isFuture) { onDayClick(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$dayOfMonth",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
