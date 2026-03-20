package com.verdant.feature.habits.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import compose.icons.TablerIcons
import compose.icons.tablericons.Archive
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.ChevronLeft
import compose.icons.tablericons.ChevronRight
import compose.icons.tablericons.DotsVertical
import compose.icons.tablericons.Pencil
import compose.icons.tablericons.Trash
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.designsystem.component.HabitContributionGrid
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

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

            // ── Top bar ───────────────────────────────────────────────────────
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(TablerIcons.ArrowLeft, "Back")
                    }
                },
                title = {
                    if (habit != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(habit.icon, style = MaterialTheme.typography.titleLarge)
                            Column {
                                Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val habitLabel = habit.label
                                if (!habitLabel.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(habitColor.copy(alpha = 0.15f)).padding(horizontal = 5.dp, vertical = 1.dp),
                                    ) {
                                        Text(habitLabel, style = MaterialTheme.typography.labelSmall, color = habitColor, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
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
                                leadingIcon = { Icon(TablerIcons.Trash, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; viewModel.deleteHabit(); onNavigateBack() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )

            if (state.isLoading || habit == null) return@Column

            // ── Stats row ─────────────────────────────────────────────────────
            StatsRow(
                currentStreak = state.currentStreak,
                longestStreak = state.longestStreak,
                completionRate = state.completionRate,
                totalEntries = state.totalEntries,
                averageValue = state.averageValue,
                unit = habit.unit,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ── Tabs ──────────────────────────────────────────────────────────
            ScrollableTabRow(selectedTabIndex = state.selectedTab, edgePadding = 0.dp, divider = {}, containerColor = Color.Transparent) {
                Tab(selected = state.selectedTab == 0, onClick = { viewModel.onTabSelected(0) }, text = { Text("History") })
                Tab(selected = state.selectedTab == 1, onClick = { viewModel.onTabSelected(1) }, text = { Text("Calendar") })
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                when (state.selectedTab) {
                    0 -> {
                        // ── Contribution grid ─────────────────────────────────
                        item {
                            Text(
                                "Last 12 weeks",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                            )
                            HabitContributionGrid(
                                cells = state.cells,
                                habitColor = habitColor,
                                weeks = 12,
                                onCellClick = { date ->
                                    if (!date.isAfter(LocalDate.now())) viewModel.onCellTapped(date)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }

                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }

                        item {
                            Text(
                                "Recent entries",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                            )
                        }

                        if (state.entries.isEmpty()) {
                            item {
                                Text(
                                    "No entries yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        } else {
                            items(state.entries) { entry ->
                                EntryListRow(
                                    entry = entry,
                                    habit = habit,
                                    habitColor = habitColor,
                                    onClick = { viewModel.onEntryTapped(entry) },
                                )
                            }
                        }
                    }
                    1 -> {
                        // ── Calendar view ─────────────────────────────────────
                        item {
                            MonthCalendar(
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
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Retroactive logging bottom sheet ─────────────────────────────────────
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
                    onSaveBinary = { completed -> viewModel.retroLogBinary(state.retroDate!!, completed); viewModel.onDismissRetroSheet() },
                    onSaveQuantitative = { value -> viewModel.retroSetQuantitative(state.retroDate!!, value); viewModel.onDismissRetroSheet() },
                    onSkip = { viewModel.retroSkip(state.retroDate!!); viewModel.onDismissRetroSheet() },
                    onDelete = { state.retroEntry?.let { viewModel.retroDeleteEntry(it) }; viewModel.onDismissRetroSheet() },
                    onDismiss = viewModel::onDismissRetroSheet,
                )
            }
        }
    }
}

// ── Stats Row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(
    currentStreak: Int,
    longestStreak: Int,
    completionRate: Float,
    totalEntries: Int,
    averageValue: Double?,
    unit: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard("🔥", "$currentStreak", "Current streak", "days")
        StatCard("🏆", "$longestStreak", "Best streak", "days")
        StatCard("✅", "${(completionRate * 100).toInt()}%", "30-day rate", "")
        StatCard("📊", "$totalEntries", "Total done", "")
        if (averageValue != null) {
            StatCard("📈", "%.1f".format(averageValue), "Average", unit ?: "")
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, suffix: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.width(90.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (suffix.isNotBlank()) Text(suffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Entry list row ────────────────────────────────────────────────────────────

@Composable
private fun EntryListRow(
    entry: HabitEntry,
    habit: com.verdant.core.model.Habit,
    habitColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Dot indicator
        Box(
            modifier = Modifier.size(10.dp).clip(CircleShape).background(
                when {
                    entry.skipped -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    entry.completed -> habitColor
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            val detail = when {
                entry.skipped -> "Skipped"
                entry.completed && habit.trackingType == TrackingType.BINARY -> "Completed"
                entry.value != null -> "${entry.value!!.toDisplayStr()} ${habit.unit.orEmpty()}"
                entry.completed -> "Done"
                else -> "Not completed"
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (entry.skipped) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("Skipped", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Month Calendar ────────────────────────────────────────────────────────────

@Composable
private fun MonthCalendar(
    month: LocalDate,
    cells: List<DayCell>,
    habitColor: Color,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellMap = cells.associateBy { it.date }
    val today = LocalDate.now()
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfWeek = month.withDayOfMonth(1).dayOfWeek.value // 1=Mon..7=Sun
    val totalCells = firstDayOfWeek - 1 + daysInMonth
    val rows = ceil(totalCells / 7.0).toInt()

    Column(modifier = modifier) {
        // Month header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) { Icon(TablerIcons.ChevronLeft, "Previous month") }
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val isCurrentMonth = !month.plusMonths(1).isAfter(today.withDayOfMonth(1))
            IconButton(onClick = onNextMonth, enabled = isCurrentMonth) {
                Icon(TablerIcons.ChevronRight, "Next month", tint = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Day grid
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - (firstDayOfWeek - 2) // 1-based
                    if (dayOfMonth < 1 || dayOfMonth > daysInMonth) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = month.withDayOfMonth(dayOfMonth)
                        val cell = cellMap[date]
                        val isFuture = date.isAfter(today)
                        val isToday = date == today
                        val intensity = cell?.intensity ?: 0f
                        val bgColor = when {
                            isFuture -> Color.Transparent
                            intensity <= 0f -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else -> habitColor.copy(alpha = 0.15f + intensity * 0.7f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(
                                    if (isToday) Modifier.border(2.dp, habitColor, CircleShape) else Modifier,
                                )
                                .clickable(enabled = !isFuture) { onDayClick(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$dayOfMonth",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Retroactive Logging Bottom Sheet ─────────────────────────────────────────

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
    var noteInput by rememberSaveable(date) { mutableStateOf(existing?.note ?: "") }

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
                            contentColor = if (isCompleted) (if (habitColor.luminance() > 0.4f) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) { Text("Done ✓") }
                    Button(
                        onClick = { onSaveBinary(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isCompleted && existing != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
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

private typealias DayCell = com.verdant.core.model.DayCell

private fun Double.toDisplayStr(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.1f".format(this)
