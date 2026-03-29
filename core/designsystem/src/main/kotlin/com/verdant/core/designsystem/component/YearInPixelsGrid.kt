package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.GridEmptyDark
import com.verdant.core.designsystem.theme.GridEmptyLight
import com.verdant.core.designsystem.theme.VerdantTheme
import com.verdant.core.model.PixelEntry
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ── Mood color scale ──────────────────────────────────────────────────────────

val MoodColor1 = Color(0xFF6B1B3A)   // 1 = terrible  (deep crimson-purple)
val MoodColor2 = Color(0xFFD4603A)   // 2 = bad        (burnt orange)
val MoodColor3 = Color(0xFFD4A838)   // 3 = neutral    (warm amber)
val MoodColor4 = Color(0xFF9E9A95)   // 4 = good       (warm stone)
val MoodColor5 = Color(0xFF2E2D2B)   // 5 = great      (warm charcoal)

fun moodScoreToColor(score: Int): Color = when (score) {
    1 -> MoodColor1
    2 -> MoodColor2
    3 -> MoodColor3
    4 -> MoodColor4
    5 -> MoodColor5
    else -> Color.Unspecified
}

fun moodScoreToEmoji(score: Int): String = when (score) {
    1 -> "😢"; 2 -> "😔"; 3 -> "😐"; 4 -> "🙂"; 5 -> "😄"
    else -> "❓"
}

// ── YearInPixelsGrid ──────────────────────────────────────────────────────────

/**
 * Year-in-Pixels mood calendar.
 *
 * Displays 52–53 week rows (Mon–Sun columns) for [year].
 * Each cell is colored by mood score (1–5), or empty when no entry exists.
 * Month abbreviations appear on the left whenever a new month begins.
 * The current day is highlighted with a border.
 * A fade-in animation plays on first composition.
 *
 * @param entries       Mood entries to display.
 * @param year          Calendar year to render.
 * @param onDayClick    Called when the user taps a past or present day cell.
 * @param overlayDates  Optional: dates to mark with a small dot overlay
 *                      (e.g. habit completion days).
 * @param modifier      Modifier applied to the outermost layout.
 */
@Composable
fun YearInPixelsGrid(
    entries: List<PixelEntry>,
    year: Int,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    overlayDates: Set<LocalDate> = emptySet(),
) {
    val today = remember { LocalDate.now() }
    val emptyColor = if (isSystemInDarkTheme()) GridEmptyDark else GridEmptyLight
    val cellSize = 13.dp
    val cellGap = 3.dp
    val labelWidth = 30.dp

    val entryMap = remember(entries) { entries.associateBy { it.date } }

    // Build week rows for the year: anchor to the Monday of the week containing Jan 1
    val weeks: List<List<LocalDate>> = remember(year) {
        val yearStart = LocalDate.of(year, 1, 1)
        val gridStart = yearStart.minusDays((yearStart.dayOfWeek.value - 1).toLong())
        val yearEnd = LocalDate.of(year, 12, 31)
        val weekCount = (gridStart.datesUntil(yearEnd.plusDays(8)).count() / 7).toInt()
        (0 until weekCount).map { w ->
            (0 until 7).map { d -> gridStart.plusDays((w * 7 + d).toLong()) }
        }
    }

    // Map: weekIndex → month label to show (when a month begins in that week)
    val monthLabels: Map<Int, String> = remember(weeks, year) {
        buildMap {
            weeks.forEachIndexed { idx, week ->
                val firstDay = week.firstOrNull { it.dayOfMonth == 1 && it.year == year }
                    ?: if (idx == 0) week.firstOrNull { it.year == year } else null
                if (firstDay != null) {
                    put(idx, firstDay.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                }
            }
        }
    }

    // Fade-in on first composition
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "yearPixelsFadeIn",
    )

    val listState = rememberLazyListState()
    LaunchedEffect(weeks) {
        if (today.year == year) {
            val todayWeekIdx = weeks.indexOfFirst { w -> w.any { it == today } }
            if (todayWeekIdx > 2) listState.scrollToItem(todayWeekIdx - 2)
        }
    }

    Column(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        // Day-of-week header (M T W T F S S)
        Row(
            modifier = Modifier.padding(start = labelWidth + cellGap),
            horizontalArrangement = Arrangement.spacedBy(cellGap),
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.size(cellSize),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(cellGap))

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(cellGap),
        ) {
            itemsIndexed(weeks) { weekIdx, week ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(cellGap),
                ) {
                    // Month label column
                    Box(modifier = Modifier.width(labelWidth)) {
                        val label = monthLabels[weekIdx]
                        if (label != null) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                        }
                    }

                    // 7 day cells
                    week.forEach { date ->
                        val inYear = date.year == year
                        val isFuture = date.isAfter(today)
                        val isToday = date == today
                        val entry = if (inYear && !isFuture) entryMap[date] else null
                        val hasOverlay = date in overlayDates

                        val bgColor = when {
                            !inYear -> Color.Transparent
                            isFuture -> emptyColor.copy(alpha = 0.3f)
                            entry != null -> moodScoreToColor(entry.moodScore)
                            else -> emptyColor
                        }

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(2.dp))
                                .background(bgColor)
                                .then(
                                    if (isToday && inYear) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(2.dp),
                                        )
                                    } else Modifier,
                                )
                                .clickable(enabled = inYear && !isFuture) { onDayClick(date) },
                            contentAlignment = Alignment.BottomEnd,
                        ) {
                            // Overlay dot (habit completion indicator)
                            if (hasOverlay && inYear && !isFuture) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .padding(bottom = 1.dp, end = 1.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.85f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Mood legend ───────────────────────────────────────────────────────────────

@Composable
fun MoodLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "😢",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        (1..5).forEach { score ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(moodScoreToColor(score)),
            )
        }
        Text(
            "😄",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private fun previewEntries(year: Int): List<PixelEntry> {
    val today = LocalDate.now()
    val start = LocalDate.of(year, 1, 1)
    val end = if (today.year == year) today else LocalDate.of(year, 12, 31)
    return start.datesUntil(end.plusDays(1)).toList()
        .filterIndexed { i, _ -> i % 3 != 0 }
        .mapIndexed { i, date -> PixelEntry(date = date, moodScore = (i % 5) + 1) }
}

@Preview(name = "YearInPixelsGrid – light", showBackground = true)
@Composable
private fun YearInPixelsGridLightPreview() {
    VerdantTheme {
        YearInPixelsGrid(
            entries = previewEntries(LocalDate.now().year),
            year = LocalDate.now().year,
            onDayClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(
    name = "YearInPixelsGrid – dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun YearInPixelsGridDarkPreview() {
    VerdantTheme(dynamicColor = false) {
        YearInPixelsGrid(
            entries = previewEntries(LocalDate.now().year),
            year = LocalDate.now().year,
            onDayClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}
