package com.verdant.feature.analytics.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.component.HabitContributionGrid
import com.verdant.core.designsystem.component.IntensityLegend
import com.verdant.core.model.Habit
import com.verdant.feature.analytics.HeatmapsState
import kotlin.math.roundToInt

@Composable
fun HeatmapsTab(
    habits: List<Habit>,
    state: HeatmapsState,
    onHabitSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (habits.isEmpty()) {
        EmptyAnalyticsState(
            message = "Add some habits to see your heatmaps",
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Habit selector — scrollable tab row at the top
        ScrollableTabRow(
            selectedTabIndex = state.selectedHabitIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            habits.forEachIndexed { index, habit ->
                Tab(
                    selected = state.selectedHabitIndex == index,
                    onClick  = { onHabitSelected(index) },
                    text     = {
                        Text(
                            text = "${habit.icon} ${habit.name}",
                            maxLines = 1,
                        )
                    },
                )
            }
        }

        HorizontalDivider()

        // Heatmap content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val habit = habits.getOrNull(state.selectedHabitIndex)
            val habitColor = habit?.let { Color(it.color) }
                ?: MaterialTheme.colorScheme.primary

            // Contribution grid
            SectionCard(title = "Activity — Past 52 Weeks") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitContributionGrid(
                        cells      = state.cells,
                        habitColor = habitColor,
                        weeks      = 52,
                        onCellClick = { /* could navigate to that day in HomeScreen */ },
                        modifier   = Modifier.fillMaxWidth(),
                    )
                    IntensityLegend(
                        color    = habitColor,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            // Stats row
            SectionCard(title = "Statistics") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    HeatmapStatItem(label = "Current\nStreak",  value = "${state.currentStreak} days")
                    HeatmapStatItem(label = "Longest\nStreak",  value = "${state.longestStreak} days")
                    HeatmapStatItem(label = "30d Rate",          value = "${(state.completionRate30d * 100).roundToInt()}%")
                    HeatmapStatItem(label = "Total\nDone",       value = "${state.totalCompletions}")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeatmapStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
