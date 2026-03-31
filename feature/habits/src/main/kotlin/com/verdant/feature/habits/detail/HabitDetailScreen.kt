package com.verdant.feature.habits.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import compose.icons.TablerIcons
import compose.icons.tablericons.Archive
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.ArrowUpRight
import compose.icons.tablericons.ChevronDown
import compose.icons.tablericons.ChevronLeft
import compose.icons.tablericons.ChevronRight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    var showMonthPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val monthPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val habit = state.habit
    val habitColor = habit?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
    val theme = remember(habit?.color) { calendarThemeFor(habit?.color ?: 0xFF2E2D2BL) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Calendar header ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                theme.headerBg,
                                theme.headerGradientEnd,
                            ),
                        ),
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(bottom = 20.dp),
            ) {
                // Top row: back arrow + options menu
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            TablerIcons.ArrowLeft,
                            contentDescription = "Back",
                            tint = theme.headerText,
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                TablerIcons.DotsVertical,
                                contentDescription = "More options",
                                tint = theme.headerText,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
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
                }

                if (state.isLoading || habit == null) return@Column

                // Year
                Text(
                    text = "${state.selectedMonth.year}",
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.headerSubtle,
                    modifier = Modifier.padding(start = 20.dp),
                )

                // Month name + habit icon/name row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMonthPicker = true }
                            .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                    ) {
                        Text(
                            text = state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = theme.headerText,
                        )
                        Icon(
                            TablerIcons.ChevronDown,
                            contentDescription = "Pick month",
                            tint = theme.headerText,
                            modifier = Modifier.size(24.dp).padding(start = 4.dp),
                        )
                    }
                    // Habit badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(theme.badgeBg)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(habit.icon, fontSize = 18.sp)
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = theme.headerText,
                        )
                    }
                }

                // Month navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { viewModel.onMonthChanged(state.selectedMonth.minusMonths(1)) },
                    ) {
                        Icon(TablerIcons.ChevronLeft, "Previous month", tint = theme.headerText)
                    }
                    // Streak indicator
                    if (state.currentStreak > 0) {
                        Text(
                            text = "${state.currentStreak} day streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.streakAccent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    val canGoNext = !state.selectedMonth.plusMonths(1).isAfter(LocalDate.now().withDayOfMonth(1))
                    IconButton(
                        onClick = {
                            val next = state.selectedMonth.plusMonths(1)
                            if (!next.isAfter(LocalDate.now().withDayOfMonth(1))) viewModel.onMonthChanged(next)
                        },
                        enabled = canGoNext,
                    ) {
                        Icon(
                            TablerIcons.ChevronRight,
                            "Next month",
                            tint = if (canGoNext) theme.headerText else theme.futureDayText,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Day-of-week headers
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.headerSubtle,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Calendar grid
                val month = state.selectedMonth
                val cellMap = state.cells.associateBy { it.date }
                val today = LocalDate.now()
                val daysInMonth = month.lengthOfMonth()
                val firstDayOfWeek = month.withDayOfMonth(1).dayOfWeek.value // 1=Mon..7=Sun

                // Previous month overflow
                val prevMonth = month.minusMonths(1)
                val prevMonthDays = prevMonth.lengthOfMonth()

                val totalCells = firstDayOfWeek - 1 + daysInMonth
                val rows = ceil(totalCells / 7.0).toInt()

                // If last row doesn't fill, we show next month days
                val totalGridCells = rows * 7

                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayOffset = cellIndex - (firstDayOfWeek - 1)
                            val dayOfMonth = dayOffset + 1

                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    // Previous month overflow
                                    dayOfMonth < 1 -> {
                                        val prevDay = prevMonthDays + dayOfMonth
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = theme.overflowBorder,
                                                    shape = CircleShape,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "$prevDay",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = theme.overflowText,
                                            )
                                        }
                                    }
                                    // Next month overflow
                                    dayOfMonth > daysInMonth -> {
                                        val nextDay = dayOfMonth - daysInMonth
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = theme.overflowBorder,
                                                    shape = CircleShape,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "$nextDay",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = theme.overflowText,
                                            )
                                        }
                                    }
                                    // Current month days
                                    else -> {
                                        val date = month.withDayOfMonth(dayOfMonth)
                                        val cell = cellMap[date]
                                        val isFuture = date.isAfter(today)
                                        val isToday = date == today
                                        val intensity = cell?.intensity ?: 0f
                                        val isSkipped = cell?.isSkipped == true

                                        val isCompleted = intensity > 0f
                                        val showBorder = !isToday && !isFuture && !isCompleted

                                        val dayBgColor by animateColorAsState(
                                            targetValue = when {
                                                isToday -> theme.todayBg
                                                isFuture -> Color.Transparent
                                                isCompleted -> theme.completedDayBg
                                                else -> Color.Transparent
                                            },
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "dayBg",
                                        )

                                        val dayTextColor = when {
                                            isToday -> theme.todayText
                                            isFuture -> theme.futureDayText
                                            isSkipped -> theme.skippedText
                                            isCompleted -> theme.completedDayText
                                            else -> theme.emptyDayText
                                        }

                                        val borderColor by animateColorAsState(
                                            targetValue = when {
                                                isSkipped -> theme.skippedBorder
                                                showBorder -> theme.emptyDayBorder
                                                else -> Color.Transparent
                                            },
                                            label = "dayBorder",
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                                .clip(CircleShape)
                                                .background(dayBgColor)
                                                .then(
                                                    if (showBorder || isSkipped) Modifier.border(
                                                        width = 1.dp,
                                                        color = borderColor,
                                                        shape = CircleShape,
                                                    ) else Modifier
                                                )
                                                .clickable(enabled = !isFuture) {
                                                    viewModel.onCellTapped(date)
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "$dayOfMonth",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                                color = dayTextColor,
                                                textDecoration = if (isSkipped) TextDecoration.LineThrough else null,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Daily Activities section ─────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Section header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Daily Activities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { viewModel.toggleShowAllEntries() }) {
                        Icon(
                            TablerIcons.ArrowUpRight,
                            contentDescription = if (state.showAllEntries) "Show less" else "View all",
                            modifier = Modifier.size(20.dp),
                            tint = if (state.showAllEntries) habitColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (state.entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = habit?.icon ?: "",
                                fontSize = 32.sp,
                            )
                            Text(
                                text = "No entries yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    val timelineColor = MaterialTheme.colorScheme.outlineVariant
                    val dotActiveColor = habitColor

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val entries = state.entries
                        items(entries.size) { index ->
                            val entry = entries[index]
                            val isLast = index == entries.size - 1

                            TimelineEntryRow(
                                entry = entry,
                                habit = habit!!,
                                habitColor = habitColor,
                                timelineColor = timelineColor,
                                isLast = isLast,
                                onClick = { viewModel.onEntryTapped(entry) },
                            )
                        }

                        // Bottom spacer
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // ── Month picker sheet ────────────────────────────────────────────────────
    if (showMonthPicker) {
        ModalBottomSheet(
            onDismissRequest = { showMonthPicker = false },
            sheetState = monthPickerSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            MonthPickerSheet(
                selectedMonth = state.selectedMonth,
                habitColor = habitColor,
                onMonthSelected = { month ->
                    viewModel.onMonthChanged(month)
                    showMonthPicker = false
                },
                onDismiss = { showMonthPicker = false },
            )
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

// ── Timeline Entry Row ───────────────────────────────────────────────────────

@Composable
private fun TimelineEntryRow(
    entry: HabitEntry,
    habit: com.verdant.core.model.Habit,
    habitColor: Color,
    timelineColor: Color,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val dotColor = when {
        entry.skipped -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        entry.completed -> habitColor
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Timeline: dot + dashed line
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(72.dp)
                .drawBehind {
                    // Dot
                    drawCircle(
                        color = dotColor,
                        radius = 5.dp.toPx(),
                        center = Offset(size.width / 2, 24.dp.toPx()),
                    )
                    // Dashed line below (unless last item)
                    if (!isLast) {
                        drawLine(
                            color = timelineColor,
                            start = Offset(size.width / 2, 34.dp.toPx()),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dashEffect,
                        )
                    }
                },
        )

        Spacer(Modifier.width(12.dp))

        // Content
        Column(
            modifier = Modifier.weight(1f).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = entry.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            val detail = when {
                entry.skipped -> "Skipped"
                entry.completed && habit.trackingType == TrackingType.BINARY -> "Completed"
                entry.value != null -> "${entry.value!!.toDisplayStr()} ${habit.unit.orEmpty()}"
                entry.completed -> "Done"
                else -> "Not completed"
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Time from createdAt
        val timeText = remember(entry.createdAt) {
            try {
                val instant = Instant.ofEpochMilli(entry.createdAt)
                val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
                val hour = localTime.hour
                val minute = localTime.minute
                val amPm = if (hour < 12) "am" else "pm"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "$displayHour:%02d $amPm".format(minute)
            } catch (_: Exception) {
                ""
            }
        }
        if (timeText.isNotEmpty()) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
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
                    ) { Text("Done") }
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

// ── Month Picker Sheet ────────────────────────────────────────────────────────

@Composable
private fun MonthPickerSheet(
    selectedMonth: LocalDate,
    habitColor: Color,
    onMonthSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    var pickerYear by remember { mutableStateOf(selectedMonth.year) }
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val contentColor = if (habitColor.luminance() > 0.4f) Color(0xFF1A1917) else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Year navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { pickerYear-- }) {
                Icon(TablerIcons.ChevronLeft, "Previous year")
            }
            Text(
                text = "$pickerYear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val canGoNext = pickerYear < today.year
            IconButton(
                onClick = { if (canGoNext) pickerYear++ },
                enabled = canGoNext,
            ) {
                Icon(TablerIcons.ChevronRight, "Next year")
            }
        }

        // Month grid (3 columns × 4 rows)
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until 3) {
                    val monthIndex = row * 3 + col + 1 // 1-based
                    val monthDate = LocalDate.of(pickerYear, monthIndex, 1)
                    val isFuture = monthDate.isAfter(today.withDayOfMonth(1))
                    val isSelected = monthDate == selectedMonth

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> habitColor
                                    else -> Color.Transparent
                                },
                            )
                            .border(
                                width = if (!isSelected && !isFuture) 1.dp else 0.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable(enabled = !isFuture) { onMonthSelected(monthDate) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = monthNames[monthIndex - 1],
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> contentColor
                                isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

private typealias DayCell = com.verdant.core.model.DayCell

private fun Double.toDisplayStr(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.1f".format(this)
