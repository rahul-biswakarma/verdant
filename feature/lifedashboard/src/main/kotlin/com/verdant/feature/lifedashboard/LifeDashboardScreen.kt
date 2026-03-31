package com.verdant.feature.lifedashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import compose.icons.TablerIcons
import com.verdant.core.model.QuestStatus
import compose.icons.tablericons.Award
import compose.icons.tablericons.ChartLine
import compose.icons.tablericons.Check
import compose.icons.tablericons.CircleCheck
import compose.icons.tablericons.Flame
import compose.icons.tablericons.Heart
import compose.icons.tablericons.MoodSmile
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Shield
import compose.icons.tablericons.Target
import compose.icons.tablericons.Trophy

const val LIFE_DASHBOARD_ROUTE = "life_dashboard"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LifeDashboardScreen(
    viewModel: LifeDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The System") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Player Card
            item {
                PlayerCard(uiState)
            }

            // Emotional State Banner
            item {
                EmotionalStateBanner(uiState)
            }

            // Stat Dimensions Grid
            item {
                StatDimensionGrid(uiState.statDimensions)
            }

            // Active Quests
            if (uiState.activeQuests.isNotEmpty()) {
                item {
                    Text(
                        "Active Quests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(uiState.activeQuests) { quest ->
                    ActiveQuestCard(
                        quest = quest,
                        onStart = { viewModel.startQuest(quest.id) },
                        onComplete = { viewModel.completeQuest(quest.id) },
                    )
                }
            }

            // Completed Quests
            if (uiState.completedQuests.isNotEmpty()) {
                item {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(uiState.completedQuests) { quest ->
                    CompletedQuestCard(quest)
                }
            }

            // Predictions
            if (uiState.predictions.isNotEmpty()) {
                item {
                    Text(
                        "Predictions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(uiState.predictions) { prediction ->
                    PredictionCardUi(prediction)
                }
            }

            // Life Forecast
            uiState.lifeForecastNarrative?.let { forecast ->
                item {
                    LifeForecastCard(forecast)
                }
            }

            // Danger Zone
            if (uiState.dangerZoneHabits.isNotEmpty()) {
                item {
                    DangerZoneCard(uiState.dangerZoneHabits)
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun PlayerCard(state: LifeDashboardUiState) {
    val profile = state.playerProfile ?: return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    TablerIcons.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Lv. ${profile.level} — ${profile.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Rank ${profile.rank.name} • ${profile.evolutionPath.name} Path",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (profile.xpToNextLevel > 0) {
                        profile.currentLevelXP.toFloat() / profile.xpToNextLevel.toFloat()
                    } else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${profile.currentLevelXP} / ${profile.xpToNextLevel} XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun EmotionalStateBanner(state: LifeDashboardUiState) {
    val stateLabel = state.emotionalState.name.lowercase().replaceFirstChar { it.uppercase() }
    val moodLabel = state.currentMood.name.lowercase().replaceFirstChar { it.uppercase() }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                TablerIcons.MoodSmile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "State: $stateLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Mood: $moodLabel • Energy: ${state.energyLevel}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatDimensionGrid(dimensions: List<StatDimension>) {
    if (dimensions.isEmpty()) return

    FlowRow(
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        dimensions.forEach { dim ->
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(dim.name, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${dim.score}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        when (dim.trend) {
                            Trend.UP -> "↑ Improving"
                            Trend.DOWN -> "↓ Declining"
                            Trend.FLAT -> "→ Stable"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (dim.trend) {
                            Trend.UP -> MaterialTheme.colorScheme.primary
                            Trend.DOWN -> MaterialTheme.colorScheme.error
                            Trend.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveQuestCard(
    quest: com.verdant.core.model.Quest,
    onStart: () -> Unit,
    onComplete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    TablerIcons.Target,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        quest.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        quest.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            quest.difficulty.name,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                    )
                    when (quest.status) {
                        QuestStatus.AVAILABLE -> FilledTonalButton(onClick = onStart) {
                            Icon(TablerIcons.PlayerPlay, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Start")
                        }
                        QuestStatus.ACTIVE -> Button(onClick = onComplete) {
                            Icon(TablerIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Complete")
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedQuestCard(quest: com.verdant.core.model.Quest) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                TablerIcons.CircleCheck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                quest.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                "+${quest.xpReward} XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun PredictionCardUi(prediction: PredictionCard) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    TablerIcons.ChartLine,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(prediction.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(prediction.summary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LifeForecastCard(narrative: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    TablerIcons.Target,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text("7-Day Forecast", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(narrative, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DangerZoneCard(habits: List<DangerZoneHabit>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    TablerIcons.Flame,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Danger Zone",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            habits.forEach { habit ->
                Text(
                    "• ${habit.habitName} — ${(habit.riskScore * 100).toInt()}% risk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
