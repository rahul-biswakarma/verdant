package com.verdant.feature.habits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.Archive
import compose.icons.tablericons.Check
import compose.icons.tablericons.Pencil
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Trash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.designsystem.component.CompletionRing
import com.verdant.core.designsystem.component.MiniContributionGrid
import com.verdant.core.model.DayCell
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import com.verdant.feature.habits.list.HabitsListViewModel

@Composable
fun HabitsScreen(
    onCreateHabit: () -> Unit,
    onHabitDetail: (String) -> Unit = {},
    onEditHabit: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HabitsListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateHabit,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(TablerIcons.Plus, contentDescription = "Create habit")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {            Text(
                text = "My Habits",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
            )
            if (state.labels.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.selectedLabel == null,
                        onClick = { viewModel.onLabelSelected(null) },
                        label = { Text("All") },
                    )
                    state.labels.forEach { label ->
                        FilterChip(
                            selected = state.selectedLabel == label.name,
                            onClick = { viewModel.onLabelSelected(label.name) },
                            label = { Text(label.name) },
                        )
                    }
                }
            }
            val displayHabits = state.filtered
            if (displayHabits.isEmpty() && !state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🌿", style = MaterialTheme.typography.displayMedium)
                        Text(
                            text = "No habits yet. Tap + to create your first habit",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(displayHabits, key = { it.id }) { habit ->
                        HabitListCard(
                            habit = habit,
                            todayEntry = state.todayEntries[habit.id],
                            streak = state.streaks[habit.id] ?: 0,
                            cells = state.recentCells[habit.id] ?: emptyList(),
                            onTap = { onHabitDetail(habit.id) },
                            onEdit = { onEditHabit(habit.id) },
                            onArchive = { viewModel.onArchiveHabit(habit) },
                            onDelete = { habitToDelete = habit },
                        )
                    }
                }
            }
        }
    }
    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete habit?") },
            text = { Text("\"${habit.name}\" and all its history will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onDeleteHabit(habit); habitToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) { Text("Cancel") }
            },
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitListCard(
    habit: Habit,
    todayEntry: HabitEntry?,
    streak: Int,
    cells: List<DayCell>,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habitColor = Color(habit.color)
    val isDark = isSystemInDarkTheme()
    var menuExpanded by remember { mutableStateOf(false) }

    val isCompleted = when (habit.trackingType) {
        TrackingType.BINARY -> todayEntry?.completed == true
        else -> todayEntry?.completed == true
    }
    val target = habit.targetValue
    val progress = when {
        habit.trackingType == TrackingType.BINARY -> if (isCompleted) 1f else 0f
        target != null && target > 0 ->
            ((todayEntry?.value ?: 0.0) / target).toFloat().coerceIn(0f, 1f)
        else -> 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "progress",
    )

    // Derive card colors from habit color
    val cardBg = if (isDark) {
        habitColor.copy(alpha = 0.15f)
    } else {
        habitColor.copy(alpha = 0.08f)
    }
    val cardBgCompleted = if (isDark) {
        habitColor.copy(alpha = 0.25f)
    } else {
        habitColor.copy(alpha = 0.14f)
    }
    val contentColor = if (isDark) Color.White else Color(0xFF1A1917)
    val subtextColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1A1917).copy(alpha = 0.55f)

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onTap, onLongClick = { menuExpanded = true }),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCompleted) cardBgCompleted else cardBg,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                habitColor.copy(alpha = if (isDark) 0.08f else 0.05f),
                            ),
                        ),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: Icon as hero
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Ring behind icon
                        CompletionRing(
                            progress = animatedProgress,
                            color = habitColor,
                            size = 56.dp,
                            strokeWidth = 4.dp,
                        )
                        Text(
                            text = habit.icon.ifEmpty { "🌱" },
                            fontSize = 24.sp,
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    // Center: info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Name
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Label + type row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val habitLabel = habit.label
                            if (!habitLabel.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(habitColor.copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = habitLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = habitColor,
                                    )
                                }
                            }
                            Text(
                                text = habit.trackingType.displayName(),
                                style = MaterialTheme.typography.labelSmall,
                                color = subtextColor,
                            )
                        }

                        // Mini contribution grid
                        if (cells.isNotEmpty()) {
                            MiniContributionGrid(
                                cells = cells,
                                habitColor = habitColor,
                            )
                        }

                        // Progress text for numeric
                        if (habit.trackingType != TrackingType.BINARY && habit.targetValue != null) {
                            val current = todayEntry?.value ?: 0.0
                            val tv = habit.targetValue ?: 0.0
                            Text(
                                text = "${current.toInt()} / ${tv.toInt()} ${habit.unit.orEmpty()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = habitColor,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Right: streak or check
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (isCompleted) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(habitColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        if (streak > 0) {
                            Text(
                                text = streak.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = habitColor,
                            )
                            Text(
                                text = if (streak == 1) "day" else "days",
                                style = MaterialTheme.typography.labelSmall,
                                color = subtextColor,
                            )
                        }
                    }
                }
            }
        }

        // Long-press context menu
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(TablerIcons.Pencil, null, Modifier.size(18.dp)) },
                onClick = { menuExpanded = false; onEdit() },
            )
            DropdownMenuItem(
                text = { Text("Archive") },
                leadingIcon = { Icon(TablerIcons.Archive, null, Modifier.size(18.dp)) },
                onClick = { menuExpanded = false; onArchive() },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(TablerIcons.Trash, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                },
                onClick = { menuExpanded = false; onDelete() },
            )
        }
    }
}

private fun TrackingType.displayName(): String = when (this) {
    TrackingType.BINARY -> "Yes / No"
    TrackingType.NUMERIC -> "Numeric"
    TrackingType.LOCATION -> "Location"
}
