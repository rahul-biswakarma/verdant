package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.Plus
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.WarmCharcoal
import com.verdant.core.designsystem.theme.VerdantTheme
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import java.time.LocalDate

/**
 * Home-screen card for a single habit.
 *
 * Displays the habit icon, name, today's progress, streak, and a quick-action
 * button. The button toggles completion for [TrackingType.BINARY] habits or
 * opens an entry sheet for quantitative types.
 *
 * @param habit        The habit to display.
 * @param todayEntry   Today's entry, or null if none recorded yet.
 * @param streak       Current active streak length (injected by the caller).
 * @param onQuickAction Invoked when the user taps the primary action button.
 * @param onTap        Invoked when the user taps the card body.
 */
@Composable
fun HabitCard(
    habit: Habit,
    todayEntry: HabitEntry?,
    onQuickAction: () -> Unit,
    onTap: () -> Unit,
    streak: Int = 0,
    modifier: Modifier = Modifier,
) {
    val habitColor = Color(habit.color.toULong())

    val isCompleted = when (habit.trackingType) {
        TrackingType.BINARY -> todayEntry?.completed == true
        else -> {
            val value = todayEntry?.value ?: 0.0
            val target = habit.targetValue ?: 0.0
            target > 0 && value >= target
        }
    }

    val targetValue = habit.targetValue
    val progress = when {
        habit.trackingType == TrackingType.BINARY ->
            if (isCompleted) 1f else 0f
        targetValue != null && targetValue > 0 ->
            ((todayEntry?.value ?: 0.0) / targetValue).toFloat().coerceIn(0f, 1f)
        else -> 0f
    }

    ElevatedCard(
        onClick = onTap,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Habit icon ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(habitColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = habit.icon.ifEmpty { "🌱" },
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Spacer(Modifier.width(16.dp))

            // ── Name, frequency label, progress ────────────────────────────
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    StreakBadge(count = streak)
                }

                Text(
                    text = frequencyLabel(habit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )

                if (habit.trackingType != TrackingType.BINARY) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.weight(1f),
                            color = habitColor,
                            trackColor = habitColor.copy(alpha = 0.18f),
                            strokeCap = StrokeCap.Round,
                        )
                        val current = todayEntry?.value ?: 0.0
                        val target = habit.targetValue ?: 0.0
                        Text(
                            text = if (target > 0) "${current.toInt()}/${target.toInt()} ${habit.unit.orEmpty()}"
                                   else "${current.toInt()} ${habit.unit.orEmpty()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // ── Quick action ────────────────────────────────────────────────
            FilledIconButton(
                onClick = onQuickAction,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isCompleted) habitColor
                                     else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isCompleted) Color.White
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = if (isCompleted) TablerIcons.Check else TablerIcons.Plus,
                    contentDescription = if (isCompleted) "Mark incomplete" else "Add entry",
                )
            }
        }
    }
}

private fun frequencyLabel(habit: Habit): String = when (habit.frequency) {
    HabitFrequency.DAILY          -> "Daily"
    HabitFrequency.WEEKDAYS       -> "Weekdays"
    HabitFrequency.WEEKENDS       -> "Weekends"
    HabitFrequency.SPECIFIC_DAYS  -> "Custom days"
    HabitFrequency.TIMES_PER_WEEK -> "× per week"
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val previewHabitBinary = Habit(
    id = "1", name = "Morning Run", description = "5 km jog", icon = "🏃",
    color = WarmCharcoal.value.toLong(), label = "Health",
    trackingType = TrackingType.BINARY,
    visualizationType = com.verdant.core.model.VisualizationType.PIXEL_GRID,
    unit = null, targetValue = null, checkpointSteps = emptyList(),
    frequency = HabitFrequency.DAILY, scheduleDays = 0x7F,
    isArchived = false, reminderEnabled = true, reminderTime = "07:00",
    reminderDays = 0x7F, sortOrder = 0, createdAt = 0L,
)

private val previewHabitQuant = Habit(
    id = "2", name = "Read", description = "Read books", icon = "📚",
    color = 0xFF2196F3, label = null,
    trackingType = TrackingType.QUANTITATIVE,
    visualizationType = com.verdant.core.model.VisualizationType.PHYSICS_JAR,
    unit = "pages", targetValue = 30.0, checkpointSteps = emptyList(),
    frequency = HabitFrequency.DAILY, scheduleDays = 0x7F,
    isArchived = false, reminderEnabled = false, reminderTime = null,
    reminderDays = 0, sortOrder = 1, createdAt = 0L,
)

private val previewEntry = HabitEntry(
    id = "e1", habitId = "2", date = LocalDate.now(),
    completed = false, value = 12.0,
    latitude = null, longitude = null, note = null, category = null,
    skipped = false, createdAt = 0L, updatedAt = 0L,
)

@Preview(name = "HabitCard Binary – light", showBackground = true)
@Composable
private fun HabitCardBinaryLightPreview() {
    VerdantTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HabitCard(habit = previewHabitBinary, todayEntry = null,  streak = 5,  onQuickAction = {}, onTap = {})
            HabitCard(habit = previewHabitBinary, todayEntry = HabitEntry(
                id = "e2", habitId = "1", date = LocalDate.now(), completed = true, value = null,
                latitude = null, longitude = null, note = null, category = null,
                skipped = false, createdAt = 0L, updatedAt = 0L,
            ), streak = 12, onQuickAction = {}, onTap = {})
        }
    }
}

@Preview(name = "HabitCard Quantitative – light", showBackground = true)
@Composable
private fun HabitCardQuantLightPreview() {
    VerdantTheme {
        HabitCard(
            habit = previewHabitQuant,
            todayEntry = previewEntry,
            streak = 3,
            onQuickAction = {},
            onTap = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(
    name = "HabitCard – dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HabitCardDarkPreview() {
    VerdantTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HabitCard(habit = previewHabitBinary, todayEntry = null,  streak = 5,  onQuickAction = {}, onTap = {})
            HabitCard(habit = previewHabitQuant,  todayEntry = previewEntry, streak = 0, onQuickAction = {}, onTap = {})
        }
    }
}
