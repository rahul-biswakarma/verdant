package com.verdant.feature.habits.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryTab(
    entries: List<HabitEntry>,
    habit: Habit,
    habitColor: Color,
    onEntryClick: (HabitEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "No entries yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                HistoryEntryRow(
                    entry = entry,
                    habit = habit,
                    habitColor = habitColor,
                    onClick = { onEntryClick(entry) },
                )
            }
        }
    }
}

@Composable
private fun HistoryEntryRow(
    entry: HabitEntry,
    habit: Habit,
    habitColor: Color,
    onClick: () -> Unit,
) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    val entryTime = Instant.ofEpochMilli(entry.createdAt)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(timeFormatter)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    when {
                        entry.skipped -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        entry.completed -> habitColor
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.date.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "at $entryTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Value / status
        Column(horizontalAlignment = Alignment.End) {
            val statusText = when {
                entry.skipped -> "Skipped"
                entry.completed && habit.trackingType == TrackingType.BINARY -> "Done"
                entry.value != null -> {
                    val v = entry.value!!
                    val display = if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
                    "$display ${habit.unit.orEmpty()}".trim()
                }
                entry.completed -> "Done"
                else -> "Not completed"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    entry.skipped -> MaterialTheme.colorScheme.onSurfaceVariant
                    entry.completed -> habitColor
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        // Skipped badge
        if (entry.skipped) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    "Skipped",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
