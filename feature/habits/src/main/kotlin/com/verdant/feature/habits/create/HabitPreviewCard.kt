package com.verdant.feature.habits.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.Settings
import compose.icons.tablericons.Bell
import compose.icons.tablericons.CalendarEvent
import compose.icons.tablericons.ChartBar
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType

@Composable
internal fun HabitPreviewCard(
    draft: HabitDraft,
    onTweakDetails: () -> Unit,
    tweakExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val habitColor = Color(draft.color)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: icon + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(habitColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(draft.icon, fontSize = 28.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (draft.label.isNotBlank()) {
                        Text(
                            text = draft.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = habitColor,
                        )
                    }
                }
            }

            // Detail chips row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Frequency
                DetailChip(
                    icon = { Icon(TablerIcons.CalendarEvent, null, Modifier.size(14.dp)) },
                    label = when (draft.frequency) {
                        HabitFrequency.DAILY -> "Daily"
                        HabitFrequency.WEEKDAYS -> "Weekdays"
                        HabitFrequency.WEEKENDS -> "Weekends"
                        HabitFrequency.SPECIFIC_DAYS -> formatScheduleDays(draft.scheduleDays)
                        HabitFrequency.TIMES_PER_WEEK -> "Weekly"
                    },
                    color = habitColor,
                )

                // Tracking type
                DetailChip(
                    icon = { Icon(TablerIcons.ChartBar, null, Modifier.size(14.dp)) },
                    label = when (draft.trackingType) {
                        TrackingType.BINARY -> "Check off"
                        TrackingType.QUANTITATIVE -> "${draft.targetValue?.fmt() ?: "?"} ${draft.unit}"
                        TrackingType.DURATION -> "${draft.targetValue?.fmt() ?: "?"} min"
                        TrackingType.FINANCIAL -> "Budget"
                        TrackingType.LOCATION -> "Location"
                        TrackingType.EMOTIONAL -> "Mood"
                        TrackingType.EVENT_DRIVEN -> "Event"
                        TrackingType.CHECKPOINT -> "Steps"
                    },
                    color = habitColor,
                )

                // Reminder
                if (draft.reminderEnabled) {
                    DetailChip(
                        icon = { Icon(TablerIcons.Bell, null, Modifier.size(14.dp)) },
                        label = draft.reminderTimes.firstOrNull() ?: "On",
                        color = habitColor,
                    )
                }
            }

            // Description if present
            if (draft.description.isNotBlank()) {
                Text(
                    text = draft.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Tweak details button
            TextButton(onClick = onTweakDetails) {
                Icon(
                    TablerIcons.Settings,
                    null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    if (tweakExpanded) "Hide details" else "Tweak details",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailChip(
    icon: @Composable () -> Unit,
    label: String,
    color: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(14.dp)) { icon() }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatScheduleDays(bitmask: Int): String {
    val days = listOf("M" to 1, "T" to 2, "W" to 4, "T" to 8, "F" to 16, "S" to 32, "S" to 64)
    return days.filter { bitmask and it.second != 0 }.joinToString("") { it.first }
}
