package com.verdant.feature.habits.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.component.CompletionRing
import com.verdant.core.designsystem.component.ProgressFill
import com.verdant.core.designsystem.component.StreakRing
import com.verdant.core.model.VisualizationType

private data class VisualizationOption(
    val type: VisualizationType,
    val label: String,
    val description: String,
)

private val visualizationOptions = listOf(
    VisualizationOption(
        type = VisualizationType.CONTRIBUTION_GRID,
        label = "Heatmap",
        description = "Daily consistency",
    ),
    VisualizationOption(
        type = VisualizationType.COMPLETION_RING,
        label = "Progress Ring",
        description = "Daily progress",
    ),
    VisualizationOption(
        type = VisualizationType.STREAK_RING,
        label = "Streak Ring",
        description = "Streak focus",
    ),
    VisualizationOption(
        type = VisualizationType.PROGRESS_FILL,
        label = "Fill Bar",
        description = "Track progress",
    ),
)

@Composable
internal fun VisualizationCarousel(
    selected: VisualizationType,
    suggested: VisualizationType,
    habitColor: Color,
    onSelect: (VisualizationType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Choose your vibe",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(visualizationOptions, key = { it.type }) { option ->
                VisualizationCard(
                    option = option,
                    isSelected = selected == option.type,
                    isSuggested = suggested == option.type,
                    habitColor = habitColor,
                    onClick = { onSelect(option.type) },
                )
            }
        }
    }
}

@Composable
private fun VisualizationCard(
    option: VisualizationOption,
    isSelected: Boolean,
    isSuggested: Boolean,
    habitColor: Color,
    onClick: () -> Unit,
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, habitColor, RoundedCornerShape(20.dp))
    } else {
        Modifier
    }

    ElevatedCard(
        modifier = Modifier
            .width(140.dp)
            .then(borderModifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // AI Pick badge
            if (isSuggested) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(habitColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .align(Alignment.Start),
                ) {
                    Text(
                        "AI Pick",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = habitColor,
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(22.dp))
            }

            // Mini preview
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (option.type) {
                    VisualizationType.CONTRIBUTION_GRID -> MiniHeatmapPreview(habitColor)
                    VisualizationType.COMPLETION_RING -> CompletionRing(
                        progress = 0.65f,
                        color = habitColor,
                        size = 56.dp,
                        strokeWidth = 5.dp,
                    )
                    VisualizationType.STREAK_RING -> StreakRing(
                        progress = 0.7f,
                        streakCount = 7,
                        color = habitColor,
                        size = 56.dp,
                        strokeWidth = 5.dp,
                    )
                    VisualizationType.PROGRESS_FILL -> ProgressFill(
                        progress = 0.65f,
                        color = habitColor,
                        size = 56.dp,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = option.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = option.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A tiny 5x7 heatmap grid for the carousel preview card.
 */
@Composable
internal fun MiniHeatmapPreview(color: Color) {
    val cells = remember5x7Cells()
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (row in 0 until 7) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (col in 0 until 5) {
                    val intensity = cells[row * 5 + col]
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                if (intensity == 0f) color.copy(alpha = 0.08f)
                                else color.copy(alpha = 0.2f + intensity * 0.6f)
                            ),
                    )
                }
            }
        }
    }
}

/** Deterministic sample intensities for the mini heatmap. */
@Composable
internal fun remember5x7Cells(): List<Float> {
    return listOf(
        0f, 0.3f, 0.8f, 1f, 0.5f,
        0.2f, 0f, 0.6f, 0.9f, 0.4f,
        0.7f, 0.5f, 0f, 0.3f, 1f,
        0f, 0.8f, 0.4f, 0.7f, 0f,
        0.9f, 0.3f, 0.6f, 0f, 0.5f,
        0.4f, 1f, 0f, 0.8f, 0.2f,
        0f, 0.5f, 0.9f, 0.3f, 0.7f,
    )
}
