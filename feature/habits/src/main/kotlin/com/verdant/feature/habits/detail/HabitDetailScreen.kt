package com.verdant.feature.habits.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import compose.icons.TablerIcons
import compose.icons.tablericons.Archive
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.DotsVertical
import compose.icons.tablericons.Pencil
import compose.icons.tablericons.Trash
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.designsystem.component.HabitContributionGrid
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDay: (String) -> Unit = {},
    onEditHabit: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            val habit = state.habit
            val habitColor = habit?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary

            // ── Top App Bar ──
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(TablerIcons.ArrowLeft, "Back")
                    }
                },
                title = {
                    if (habit != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(habit.icon, style = MaterialTheme.typography.titleLarge)
                            Text(
                                habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(TablerIcons.DotsVertical, "More options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(TablerIcons.Pencil, null, Modifier.size(18.dp)) },
                                onClick = { menuExpanded = false; habit?.let { onEditHabit(it.id) } },
                            )
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                leadingIcon = { Icon(TablerIcons.Archive, null, Modifier.size(18.dp)) },
                                onClick = { menuExpanded = false; viewModel.archiveHabit(); onNavigateBack() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(TablerIcons.Trash, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { menuExpanded = false; viewModel.deleteHabit(); onNavigateBack() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )

            if (state.isLoading || habit == null) return@Column

            // ── Full-width Contribution Grid ──
            HabitContributionGrid(
                cells = state.cells,
                habitColor = habitColor,
                weeks = 12,
                onCellClick = { date ->
                    if (!date.isAfter(LocalDate.now())) viewModel.onCellTapped(date)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ── Tab Row ──
            val tabs = DetailTab.entries
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(state.selectedTab),
                edgePadding = 0.dp,
                divider = {},
                containerColor = Color.Transparent,
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }

            // ── Tab Content ──
            when (state.selectedTab) {
                DetailTab.DETAILS -> {
                    DetailsTab(
                        currentStreak = state.currentStreak,
                        longestStreak = state.longestStreak,
                        completionRate = state.completionRate,
                        totalEntries = state.totalEntries,
                        averageValue = state.averageValue,
                        unit = habit.unit,
                        modifier = Modifier.weight(1f),
                    )
                }
                DetailTab.CALENDAR -> {
                    CalendarTab(
                        month = state.selectedMonth,
                        cells = state.cells,
                        habitColor = habitColor,
                        onPreviousMonth = { viewModel.onMonthChanged(state.selectedMonth.minusMonths(1)) },
                        onNextMonth = {
                            val next = state.selectedMonth.plusMonths(1)
                            if (!next.isAfter(LocalDate.now().withDayOfMonth(1))) viewModel.onMonthChanged(next)
                        },
                        onDayClick = { date ->
                            if (!date.isAfter(LocalDate.now())) viewModel.onCellTapped(date)
                        },
                        modifier = Modifier.weight(1f).padding(16.dp),
                    )
                }
                DetailTab.AI_COACH -> {
                    HabitCoachChat(
                        state = state.chat,
                        habitName = habit.name,
                        onInputChanged = viewModel::onChatInputChanged,
                        onSend = viewModel::sendChatMessage,
                        onRetry = viewModel::retryChatMessage,
                        onClear = viewModel::clearChat,
                        onSuggestionClick = viewModel::onSuggestionClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                DetailTab.HISTORY -> {
                    HistoryTab(
                        entries = state.allEntries,
                        habit = habit,
                        habitColor = habitColor,
                        onEntryClick = { entry -> viewModel.onEntryTapped(entry) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    // ── Retro-logging Bottom Sheet ──
    if (state.retroDate != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissRetroSheet,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            state.habit?.let { habit ->
                RetroLoggingSheet(
                    date = state.retroDate!!,
                    existing = state.retroEntry,
                    habit = habit,
                    onSaveBinary = { completed ->
                        viewModel.retroLogBinary(state.retroDate!!, completed)
                        viewModel.onDismissRetroSheet()
                    },
                    onSaveQuantitative = { value ->
                        viewModel.retroSetQuantitative(state.retroDate!!, value)
                        viewModel.onDismissRetroSheet()
                    },
                    onSkip = {
                        viewModel.retroSkip(state.retroDate!!)
                        viewModel.onDismissRetroSheet()
                    },
                    onDelete = {
                        state.retroEntry?.let { viewModel.retroDeleteEntry(it) }
                        viewModel.onDismissRetroSheet()
                    },
                    onDismiss = viewModel::onDismissRetroSheet,
                )
            }
        }
    }
}

@Composable
private fun RetroLoggingSheet(
    date: LocalDate,
    existing: HabitEntry?,
    habit: com.verdant.core.model.Habit,
    onSaveBinary: (Boolean) -> Unit,
    onSaveQuantitative: (Double) -> Unit,
    onSkip: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val habitColor = Color(habit.color)
    var valueInput by rememberSaveable(date) { mutableStateOf(existing?.value?.toDisplayStr() ?: "") }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        when (habit.trackingType) {
            TrackingType.BINARY -> {
                val isCompleted = existing?.completed == true
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSaveBinary(true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) habitColor else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isCompleted) (if (habitColor.luminance() > 0.4f) Color.Black else Color.White)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) { Text("Done ✓") }
                    Button(
                        onClick = { onSaveBinary(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isCompleted && existing != null) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) { Text("Not done") }
                }
            }
            else -> {
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Value${habit.unit?.let { " ($it)" } ?: ""}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Button(
                    onClick = { onSaveQuantitative(valueInput.toDoubleOrNull() ?: 0.0) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = valueInput.toDoubleOrNull() != null,
                    colors = ButtonDefaults.buttonColors(containerColor = habitColor),
                ) {
                    Text("Save", color = if (habitColor.luminance() > 0.4f) Color.Black else Color.White)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onSkip) { Text("Mark as skipped") }
            Spacer(Modifier.weight(1f))
            if (existing != null) {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun Double.toDisplayStr(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.1f".format(this)
