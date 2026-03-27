package com.verdant.feature.habits.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.component.CompletionRing
import com.verdant.core.designsystem.component.PhysicsJar
import com.verdant.core.designsystem.component.StreakRing
import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType

internal data class TrackingVisualizationOption(
    val trackingType: TrackingType,
    val visualizationType: VisualizationType,
    val label: String,
    val trackingLabel: String,
    val description: String,
)

private val trackingVisualizationOptions = listOf(
    TrackingVisualizationOption(
        trackingType = TrackingType.BINARY,
        visualizationType = VisualizationType.CONTRIBUTION_GRID,
        label = "Heatmap",
        trackingLabel = "Daily check-off",
        description = "Track consistency",
    ),
    TrackingVisualizationOption(
        trackingType = TrackingType.QUANTITATIVE,
        visualizationType = VisualizationType.PHYSICS_JAR,
        label = "Liquid Jar",
        trackingLabel = "Count value",
        description = "Fill it up",
    ),
    TrackingVisualizationOption(
        trackingType = TrackingType.DURATION,
        visualizationType = VisualizationType.COMPLETION_RING,
        label = "Progress Ring",
        trackingLabel = "Track time",
        description = "Daily progress",
    ),
    TrackingVisualizationOption(
        trackingType = TrackingType.FINANCIAL,
        visualizationType = VisualizationType.COMPLETION_RING,
        label = "Budget Ring",
        trackingLabel = "Track spending",
        description = "Stay on budget",
    ),
    TrackingVisualizationOption(
        trackingType = TrackingType.BINARY,
        visualizationType = VisualizationType.STREAK_RING,
        label = "Streak Ring",
        trackingLabel = "Build streaks",
        description = "Streak focus",
    ),
)

@Composable
internal fun TrackingVisualizationPicker(
    selectedTrackingType: TrackingType,
    selectedVisualization: VisualizationType,
    habitColor: Color,
    unit: String,
    targetValue: Double?,
    onOptionSelected: (TrackingType, VisualizationType) -> Unit,
    onUnitChange: (String) -> Unit,
    onTargetChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "How will you track it?",
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
            items(trackingVisualizationOptions, key = { "${it.trackingType}_${it.visualizationType}" }) { option ->
                TrackingVisualizationCard(
                    option = option,
                    isSelected = selectedTrackingType == option.trackingType
                            && selectedVisualization == option.visualizationType,
                    habitColor = habitColor,
                    onClick = { onOptionSelected(option.trackingType, option.visualizationType) },
                )
            }
        }

        // Conditional target/unit fields below the carousel
        AnimatedVisibility(
            visible = selectedTrackingType == TrackingType.QUANTITATIVE
                    || selectedTrackingType == TrackingType.DURATION
                    || selectedTrackingType == TrackingType.FINANCIAL,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (selectedTrackingType) {
                    TrackingType.QUANTITATIVE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = targetValue?.fmt() ?: "",
                                onValueChange = { onTargetChange(it.toDoubleOrNull()) },
                                label = { Text("Target") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                            OutlinedTextField(
                                value = unit,
                                onValueChange = onUnitChange,
                                label = { Text("Unit") },
                                placeholder = { Text("e.g., glasses, pages") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                            )
                        }
                    }
                    TrackingType.DURATION -> {
                        OutlinedTextField(
                            value = targetValue?.fmt() ?: "",
                            onValueChange = { onTargetChange(it.toDoubleOrNull()) },
                            label = { Text("Target (minutes)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    TrackingType.FINANCIAL -> {
                        OutlinedTextField(
                            value = targetValue?.fmt() ?: "",
                            onValueChange = { onTargetChange(it.toDoubleOrNull()) },
                            label = { Text("Budget") },
                            prefix = { Text("\u20B9") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun TrackingVisualizationCard(
    option: TrackingVisualizationOption,
    isSelected: Boolean,
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
            // Tracking label badge at top
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(habitColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .align(Alignment.Start),
            ) {
                Text(
                    option.trackingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = habitColor,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Mini chart preview
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (option.visualizationType) {
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
                    VisualizationType.PHYSICS_JAR -> PhysicsJar(
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
