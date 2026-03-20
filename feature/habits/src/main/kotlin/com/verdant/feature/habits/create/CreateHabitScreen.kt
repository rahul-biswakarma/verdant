package com.verdant.feature.habits.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.ChevronDown
import compose.icons.tablericons.ChevronUp
import compose.icons.tablericons.Microphone
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Stars
import compose.icons.tablericons.X
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateHabitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Ensure a draft always exists so the form is visible from the start
    LaunchedEffect(Unit) { viewModel.ensureDraft() }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onNavigateBack()
    }

    val draft = uiState.draft ?: return
    val habitColor = Color(draft.color)
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ─────────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Text(
                        text = "New Habit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(TablerIcons.X, contentDescription = "Close")
                    }
                },
                actions = {
                    // AI assist button
                    IconButton(onClick = { /* TODO: voice input */ }) {
                        Icon(TablerIcons.Microphone, contentDescription = "Voice input")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            // ── Scrollable form ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── AI Assist Bar ───────────────────────────────────────────
                AiAssistBar(
                    input = uiState.aiInput,
                    isLoading = uiState.isAiLoading,
                    error = uiState.aiError,
                    onInputChange = viewModel::onAiInputChange,
                    onSubmit = {
                        keyboardController?.hide()
                        viewModel.onSubmitAiInput()
                    },
                )

                // ── Quick templates ─────────────────────────────────────────
                QuickTemplateChips(
                    onTemplateSelected = viewModel::onTemplateSelected,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // ── Section 1: What? ────────────────────────────────────────
                SectionHeader(title = "What's your habit?")

                OutlinedTextField(
                    value = draft.name,
                    onValueChange = viewModel::onDraftNameChange,
                    placeholder = { Text("e.g., Take medicine, Read, Run...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                )

                // Icon + Color row
                IconColorRow(
                    selectedIcon = draft.icon,
                    selectedColor = draft.color,
                    onIconChange = viewModel::onDraftIconChange,
                    onColorChange = viewModel::onDraftColorChange,
                )

                // ── Section 2: When? ────────────────────────────────────────
                SectionHeader(title = "When?")

                FrequencySelector(
                    frequency = draft.frequency,
                    scheduleDays = draft.scheduleDays,
                    onFrequencyChange = viewModel::onDraftFrequencyChange,
                    onScheduleDaysChange = viewModel::onDraftScheduleDaysChange,
                )

                // ── Section 3: Track how? (collapsible) ─────────────────────
                CollapsibleSection(
                    title = "Track how?",
                    subtitle = when (draft.trackingType) {
                        TrackingType.BINARY -> "Just check it off"
                        TrackingType.QUANTITATIVE -> "Count ${draft.unit.ifBlank { "value" }}${draft.targetValue?.let { " (target: ${it.fmt()})" } ?: ""}"
                        TrackingType.DURATION -> "Track time${draft.targetValue?.let { " (target: ${it.fmt()} min)" } ?: ""}"
                        TrackingType.FINANCIAL -> "Track spending"
                        TrackingType.LOCATION -> "Check in at location"
                    },
                    expanded = uiState.trackingExpanded,
                    onToggle = viewModel::onToggleTracking,
                ) {
                    TrackingTypeSection(
                        trackingType = draft.trackingType,
                        unit = draft.unit,
                        targetValue = draft.targetValue,
                        onTypeChange = viewModel::onDraftTrackingTypeChange,
                        onUnitChange = viewModel::onDraftUnitChange,
                        onTargetChange = viewModel::onDraftTargetValueChange,
                    )
                }

                // ── Section 4: Remind me? (collapsible) ─────────────────────
                CollapsibleSection(
                    title = "Remind me",
                    subtitle = if (draft.reminderEnabled) {
                        draft.reminderTimes.joinToString(" & ")
                    } else "Off",
                    expanded = uiState.reminderExpanded,
                    onToggle = viewModel::onToggleReminder,
                ) {
                    ReminderSection(
                        enabled = draft.reminderEnabled,
                        times = draft.reminderTimes,
                        onEnabledChange = viewModel::onDraftReminderEnabledChange,
                        onTimesChange = viewModel::onDraftReminderTimesChange,
                    )
                }

                // ── Section 5: More options (collapsible) ───────────────────
                CollapsibleSection(
                    title = "More options",
                    subtitle = listOfNotNull(
                        draft.description.takeIf { it.isNotBlank() }?.let { "Notes" },
                        draft.label.takeIf { it.isNotBlank() },
                        draft.streakGoal?.let { "Goal: $it days" },
                    ).joinToString(" · ").ifBlank { "Description, label, streak goal" },
                    expanded = uiState.moreOptionsExpanded,
                    onToggle = viewModel::onToggleMoreOptions,
                ) {
                    MoreOptionsSection(
                        description = draft.description,
                        label = draft.label,
                        streakGoal = draft.streakGoal,
                        onDescriptionChange = viewModel::onDraftDescriptionChange,
                        onLabelChange = viewModel::onDraftLabelChange,
                        onStreakGoalChange = viewModel::onDraftStreakGoalChange,
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Save button ─────────────────────────────────────────────────
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = viewModel::onSaveHabit,
                    enabled = draft.name.isNotBlank() && !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = habitColor,
                        contentColor = if (habitColor.luminance() > 0.4f) Color.Black else Color.White,
                    ),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = if (habitColor.luminance() > 0.4f) Color.Black else Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Start tracking", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ── AI Assist Bar ───────────────────────────────────────────────────────────

@Composable
private fun AiAssistBar(
    input: String,
    isLoading: Boolean,
    error: String?,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(TablerIcons.Stars, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                Text("Describe your habit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("e.g., Take medicine every morning and evening before food", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                )
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onSubmit, modifier = Modifier.size(36.dp)) {
                        Icon(TablerIcons.Stars, "Parse", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Quick Template Chips ────────────────────────────────────────────────────

@Composable
private fun QuickTemplateChips(
    onTemplateSelected: (HabitTemplate) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        popularTemplates.forEach { template ->
            FilterChip(
                selected = false,
                onClick = { onTemplateSelected(template) },
                label = { Text(template.name, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Text(template.icon, fontSize = 14.sp) },
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}

// ── Icon + Color Row ────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconColorRow(
    selectedIcon: String,
    selectedColor: Long,
    onIconChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
) {
    var showIconPicker by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Icon selector
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(selectedColor).copy(alpha = 0.15f))
                .clickable { showIconPicker = !showIconPicker },
            contentAlignment = Alignment.Center,
        ) {
            Text(selectedIcon, fontSize = 28.sp)
        }

        // Color dots
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            COLOR_PRESETS.forEach { colorLong ->
                val isSelected = selectedColor == colorLong
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .then(
                            if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorChange(colorLong) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(TablerIcons.Check, null, Modifier.size(14.dp), tint = Color.White)
                    }
                }
            }
        }
    }

    // Expandable icon picker
    AnimatedVisibility(visible = showIconPicker) {
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ICON_PRESETS.forEach { icon ->
                val isSelected = selectedIcon == icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(selectedColor).copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable {
                            onIconChange(icon)
                            showIconPicker = false
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(icon, fontSize = 22.sp)
                }
            }
        }
    }
}

// ── Frequency Selector ──────────────────────────────────────────────────────

@Composable
private fun FrequencySelector(
    frequency: HabitFrequency,
    scheduleDays: Int,
    onFrequencyChange: (HabitFrequency) -> Unit,
    onScheduleDaysChange: (Int) -> Unit,
) {
    // Segmented buttons: Daily / Weekdays / Weekends / Custom
    val options = listOf(
        HabitFrequency.DAILY to "Daily",
        HabitFrequency.WEEKDAYS to "Weekdays",
        HabitFrequency.WEEKENDS to "Weekends",
        HabitFrequency.SPECIFIC_DAYS to "Custom",
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (freq, label) ->
            val isSelected = frequency == freq
            FilterChip(
                selected = isSelected,
                onClick = {
                    onFrequencyChange(freq)
                    when (freq) {
                        HabitFrequency.DAILY -> onScheduleDaysChange(0x7F)
                        HabitFrequency.WEEKDAYS -> onScheduleDaysChange(0x1F)
                        HabitFrequency.WEEKENDS -> onScheduleDaysChange(0x60)
                        else -> {} // keep current
                    }
                },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ),
            )
        }
    }

    // Day selector for Custom
    AnimatedVisibility(visible = frequency == HabitFrequency.SPECIFIC_DAYS) {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SCHEDULE_DAYS.forEach { (dayLabel, bit) ->
                val isActive = scheduleDays and bit != 0
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            val newDays = if (isActive) scheduleDays and bit.inv() else scheduleDays or bit
                            onScheduleDaysChange(newDays)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Collapsible Section ─────────────────────────────────────────────────────

@Composable
private fun CollapsibleSection(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(
                if (expanded) TablerIcons.ChevronUp else TablerIcons.ChevronDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

// ── Tracking Type Section ───────────────────────────────────────────────────

@Composable
private fun TrackingTypeSection(
    trackingType: TrackingType,
    unit: String,
    targetValue: Double?,
    onTypeChange: (TrackingType) -> Unit,
    onUnitChange: (String) -> Unit,
    onTargetChange: (Double?) -> Unit,
) {
    val types = listOf(
        TrackingType.BINARY to "Check off",
        TrackingType.QUANTITATIVE to "Count",
        TrackingType.DURATION to "Time",
        TrackingType.FINANCIAL to "Money",
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        types.forEach { (type, label) ->
            FilterChip(
                selected = trackingType == type,
                onClick = { onTypeChange(type) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ),
            )
        }
    }

    // Conditional fields based on type
    when (trackingType) {
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
                prefix = { Text("₹") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        else -> {} // BINARY and LOCATION need no extra fields
    }
}

// ── Reminder Section ────────────────────────────────────────────────────────

@Composable
private fun ReminderSection(
    enabled: Boolean,
    times: List<String>,
    onEnabledChange: (Boolean) -> Unit,
    onTimesChange: (List<String>) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Enable reminders", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }

    AnimatedVisibility(visible = enabled) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            times.forEachIndexed { index, time ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { newTime ->
                            val updated = times.toMutableList()
                            updated[index] = newTime
                            onTimesChange(updated)
                        },
                        label = { Text("Time ${index + 1}") },
                        placeholder = { Text("HH:mm") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                    )
                    if (times.size > 1) {
                        IconButton(
                            onClick = {
                                val updated = times.toMutableList()
                                updated.removeAt(index)
                                onTimesChange(updated)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(TablerIcons.X, "Remove", Modifier.size(16.dp))
                        }
                    }
                }
            }
            TextButton(
                onClick = { onTimesChange(times + "08:00") },
            ) {
                Icon(TablerIcons.Plus, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add another time")
            }
        }
    }
}

// ── More Options Section ────────────────────────────────────────────────────

@Composable
private fun MoreOptionsSection(
    description: String,
    label: String,
    streakGoal: Int?,
    onDescriptionChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onStreakGoalChange: (Int?) -> Unit,
) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("Notes") },
        placeholder = { Text("e.g., Before food, After workout...") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        minLines = 2,
        maxLines = 3,
    )
    OutlinedTextField(
        value = label,
        onValueChange = onLabelChange,
        label = { Text("Category / Label") },
        placeholder = { Text("e.g., Health, Fitness...") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
    )
    OutlinedTextField(
        value = streakGoal?.toString() ?: "",
        onValueChange = { onStreakGoalChange(it.toIntOrNull()) },
        label = { Text("Streak goal (days)") },
        placeholder = { Text("e.g., 30") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

// ── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun Double.fmt(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.1f".format(this)
