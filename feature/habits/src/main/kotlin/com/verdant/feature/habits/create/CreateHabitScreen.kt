package com.verdant.feature.habits.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.Label
import com.verdant.core.model.TrackingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateHabitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val labels by viewModel.labels.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onNavigateBack()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── AI input ─────────────────────────────────────────────────
                AiInputSection(
                    input = uiState.aiInput,
                    isLoading = uiState.isAiLoading,
                    error = uiState.aiError,
                    onInputChange = viewModel::onAiInputChange,
                    onSubmit = viewModel::onSubmitAiInput,
                )

                // ── Template picker ───────────────────────────────────────────
                AnimatedVisibility(visible = uiState.draft == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Or start from a template",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TemplatePicker(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = viewModel::onCategorySelected,
                            onTemplateSelected = viewModel::onTemplateSelected,
                        )
                    }
                }

                // ── Preview card ──────────────────────────────────────────────
                AnimatedVisibility(
                    visible = uiState.draft != null,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut(),
                ) {
                    uiState.draft?.let { draft ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Preview",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = viewModel::onDismissPreview) {
                                    Text("Clear")
                                }
                            }

                            HabitPreviewCard(draft = draft)

                            // Edit details toggle
                            TextButton(
                                onClick = viewModel::onToggleFullForm,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFullFormExpanded)
                                        Icons.Rounded.KeyboardArrowUp
                                    else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (uiState.isFullFormExpanded) "Hide details" else "Edit details")
                            }

                            // Full edit form
                            AnimatedVisibility(
                                visible = uiState.isFullFormExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                FullEditForm(
                                    draft = draft,
                                    labels = labels,
                                    onNameChange = viewModel::onDraftNameChange,
                                    onDescriptionChange = viewModel::onDraftDescriptionChange,
                                    onIconChange = viewModel::onDraftIconChange,
                                    onColorChange = viewModel::onDraftColorChange,
                                    onLabelChange = viewModel::onDraftLabelChange,
                                    onTrackingTypeChange = viewModel::onDraftTrackingTypeChange,
                                    onUnitChange = viewModel::onDraftUnitChange,
                                    onTargetValueChange = viewModel::onDraftTargetValueChange,
                                    onFrequencyChange = viewModel::onDraftFrequencyChange,
                                    onScheduleDaysChange = viewModel::onDraftScheduleDaysChange,
                                    onReminderEnabledChange = viewModel::onDraftReminderEnabledChange,
                                    onReminderTimeChange = viewModel::onDraftReminderTimeChange,
                                    onReminderDaysChange = viewModel::onDraftReminderDaysChange,
                                    onStreakGoalChange = viewModel::onDraftStreakGoalChange,
                                    onCreateNewLabel = viewModel::onCreateNewLabel,
                                )
                            }

                            // Start tracking button
                            Button(
                                onClick = viewModel::onSaveHabit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                enabled = !uiState.isSaving && draft.name.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(draft.color),
                                    contentColor = if (Color(draft.color).luminance() > 0.4f)
                                        Color.Black else Color.White,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                } else {
                                    Text(
                                        text = "Start tracking",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── AI Input Section ──────────────────────────────────────────────────────────

@Composable
private fun AiInputSection(
    input: String,
    isLoading: Boolean,
    error: String?,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "Describe your habit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "e.g. \"I want to run 5km every morning\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { /* voice input placeholder */ }) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Voice input (coming soon)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        onSubmit()
                    },
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = false,
                maxLines = 3,
            )

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            FilledTonalButton(
                onClick = {
                    keyboard?.hide()
                    onSubmit()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = input.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(10.dp),
            ) {
                AnimatedContent(
                    targetState = isLoading,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ai_button_state",
                ) { loading ->
                    if (loading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Parsing…")
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Text("Parse with AI")
                        }
                    }
                }
            }
        }
    }
}

// ── Template Picker ───────────────────────────────────────────────────────────

@Composable
private fun TemplatePicker(
    selectedCategory: TemplateCategory,
    onCategorySelected: (TemplateCategory) -> Unit,
    onTemplateSelected: (HabitTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryIndex = templateCategories.indexOf(selectedCategory)
    val templates = habitTemplates[selectedCategory] ?: emptyList()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScrollableTabRow(
            selectedTabIndex = categoryIndex,
            edgePadding = 0.dp,
            divider = {},
            containerColor = Color.Transparent,
        ) {
            templateCategories.forEachIndexed { index, category ->
                Tab(
                    selected = index == categoryIndex,
                    onClick = { onCategorySelected(category) },
                    text = {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (index == categoryIndex) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            templates.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { template ->
                        TemplateCard(
                            template = template,
                            onClick = { onTemplateSelected(template) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: HabitTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habitColor = Color(template.color)

    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(habitColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(template.icon, style = MaterialTheme.typography.titleMedium)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = trackingTypeLabel(template.trackingType, template.unit, template.targetValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

// ── Habit Preview Card ────────────────────────────────────────────────────────

@Composable
private fun HabitPreviewCard(
    draft: HabitDraft,
    modifier: Modifier = Modifier,
) {
    val habitColor = Color(draft.color)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Color accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(120.dp)
                    .background(
                        habitColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Icon + name row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(habitColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = draft.icon.ifEmpty { "🌱" },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draft.name.ifEmpty { "New habit" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (draft.label.isNotBlank()) {
                            LabelPill(label = draft.label, color = habitColor)
                        }
                    }

                    // Color swatch
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(habitColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Details row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PreviewDetailItem(
                        label = "Tracking",
                        value = trackingTypeLabel(draft.trackingType, draft.unit, draft.targetValue),
                        modifier = Modifier.weight(1f),
                    )
                    PreviewDetailItem(
                        label = "Schedule",
                        value = frequencyLabel(draft.frequency),
                        modifier = Modifier.weight(1f),
                    )
                }

                if (draft.reminderEnabled && draft.reminderTime.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = habitColor,
                        )
                        Text(
                            text = "Reminder at ${draft.reminderTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PreviewDetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Full Edit Form ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FullEditForm(
    draft: HabitDraft,
    labels: List<Label>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    onLabelChange: (String) -> Unit,
    onTrackingTypeChange: (TrackingType) -> Unit,
    onUnitChange: (String) -> Unit,
    onTargetValueChange: (Double?) -> Unit,
    onFrequencyChange: (HabitFrequency) -> Unit,
    onScheduleDaysChange: (Int) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (String) -> Unit,
    onReminderDaysChange: (Int) -> Unit,
    onStreakGoalChange: (Int?) -> Unit,
    onCreateNewLabel: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newLabelInput by remember { mutableStateOf("") }
    var showNewLabelField by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Name ──────────────────────────────────────────────────────────
            FormSection(title = "Name") {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Habit name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    shape = RoundedCornerShape(10.dp),
                )
            }

            // ── Description ───────────────────────────────────────────────────
            FormSection(title = "Description (optional)") {
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Why does this habit matter to you?") },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    shape = RoundedCornerShape(10.dp),
                )
            }

            // ── Icon ──────────────────────────────────────────────────────────
            FormSection(title = "Icon") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ICON_PRESETS.forEach { emoji ->
                        val isSelected = emoji == draft.icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(draft.color).copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color(draft.color) else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .clickable { onIconChange(emoji) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emoji, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // ── Color ─────────────────────────────────────────────────────────
            FormSection(title = "Color") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    COLOR_PRESETS.forEach { colorLong ->
                        val color = Color(colorLong)
                        val isSelected = colorLong == draft.color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                )
                                .clickable { onColorChange(colorLong) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (color.luminance() > 0.4f) Color.Black else Color.White,
                                )
                            }
                        }
                    }
                }
            }

            // ── Label ─────────────────────────────────────────────────────────
            FormSection(title = "Label") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        labels.forEach { label ->
                            FilterChip(
                                selected = draft.label == label.name,
                                onClick = {
                                    onLabelChange(if (draft.label == label.name) "" else label.name)
                                },
                                label = { Text(label.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(label.color).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(label.color),
                                ),
                            )
                        }
                        // Add new label chip
                        FilterChip(
                            selected = false,
                            onClick = { showNewLabelField = !showNewLabelField },
                            label = { Text(if (showNewLabelField) "Cancel" else "+ New label") },
                        )
                    }

                    if (showNewLabelField) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = newLabelInput,
                                onValueChange = { newLabelInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Label name") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            )
                            FilledTonalButton(
                                onClick = {
                                    if (newLabelInput.isNotBlank()) {
                                        onCreateNewLabel(newLabelInput, draft.color)
                                        newLabelInput = ""
                                        showNewLabelField = false
                                    }
                                },
                                enabled = newLabelInput.isNotBlank(),
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }

            // ── Tracking type ─────────────────────────────────────────────────
            FormSection(title = "Tracking type") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TrackingType.entries.forEach { type ->
                            FilterChip(
                                selected = draft.trackingType == type,
                                onClick = { onTrackingTypeChange(type) },
                                label = { Text(type.displayName()) },
                            )
                        }
                    }

                    // Quantitative / Duration / Financial targets
                    if (draft.trackingType != TrackingType.BINARY && draft.trackingType != TrackingType.LOCATION) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = draft.targetValue?.let {
                                    if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                                } ?: "",
                                onValueChange = { text ->
                                    onTargetValueChange(text.toDoubleOrNull())
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Target") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next,
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                            )
                            OutlinedTextField(
                                value = draft.unit,
                                onValueChange = onUnitChange,
                                modifier = Modifier.weight(1f),
                                label = { Text("Unit") },
                                placeholder = {
                                    Text(
                                        when (draft.trackingType) {
                                            TrackingType.DURATION -> "min"
                                            TrackingType.FINANCIAL -> "USD"
                                            else -> "e.g. km"
                                        },
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(10.dp),
                            )
                        }
                    }
                }
            }

            // ── Schedule ──────────────────────────────────────────────────────
            FormSection(title = "Schedule") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FrequencyDropdown(
                        selected = draft.frequency,
                        onSelected = onFrequencyChange,
                    )

                    if (draft.frequency == HabitFrequency.SPECIFIC_DAYS ||
                        draft.frequency == HabitFrequency.TIMES_PER_WEEK
                    ) {
                        DaySelector(
                            selectedDays = draft.scheduleDays,
                            onDaysChange = onScheduleDaysChange,
                        )
                    }
                }
            }

            // ── Reminder ──────────────────────────────────────────────────────
            FormSection(title = "Reminder") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (draft.reminderEnabled)
                                    Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (draft.reminderEnabled)
                                    Color(draft.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (draft.reminderEnabled) "On" else "Off",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(
                            checked = draft.reminderEnabled,
                            onCheckedChange = onReminderEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(draft.color),
                            ),
                        )
                    }

                    AnimatedVisibility(visible = draft.reminderEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = draft.reminderTime,
                                onValueChange = onReminderTimeChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Time (HH:mm)") },
                                placeholder = { Text("08:00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            )
                            DaySelector(
                                selectedDays = draft.reminderDays,
                                onDaysChange = onReminderDaysChange,
                            )
                        }
                    }
                }
            }

            // ── Streak goal ───────────────────────────────────────────────────
            FormSection(title = "Streak goal (optional)") {
                OutlinedTextField(
                    value = draft.streakGoal?.toString() ?: "",
                    onValueChange = { text ->
                        onStreakGoalChange(text.toIntOrNull()?.takeIf { it > 0 })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 30 days") },
                    suffix = { Text("days") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(10.dp),
                )
            }
        }
    }
}

// ── Form helpers ──────────────────────────────────────────────────────────────

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun DaySelector(
    selectedDays: Int,
    onDaysChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SCHEDULE_DAYS.forEach { (label, bit) ->
            val isSelected = selectedDays and bit != 0
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable { onDaysChange(selectedDays xor bit) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyDropdown(
    selected: HabitFrequency,
    onSelected: (HabitFrequency) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = frequencyLabel(selected),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(10.dp),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            HabitFrequency.entries.forEach { freq ->
                DropdownMenuItem(
                    text = { Text(frequencyLabel(freq)) },
                    onClick = {
                        onSelected(freq)
                        expanded = false
                    },
                    trailingIcon = {
                        if (freq == selected) {
                            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                        }
                    },
                )
            }
        }
    }
}

// ── Label / helper functions ──────────────────────────────────────────────────

private fun trackingTypeLabel(type: TrackingType, unit: String?, targetValue: Double?): String =
    when (type) {
        TrackingType.BINARY -> "Yes / No"
        TrackingType.QUANTITATIVE -> {
            val target = targetValue?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            }
            if (target != null && unit != null) "$target $unit" else unit ?: "Quantity"
        }
        TrackingType.DURATION -> {
            val target = targetValue?.toInt()
            if (target != null) "$target ${unit ?: "min"}" else unit ?: "Duration"
        }
        TrackingType.LOCATION -> "Location"
        TrackingType.FINANCIAL -> "Financial"
    }

private fun frequencyLabel(freq: HabitFrequency): String = when (freq) {
    HabitFrequency.DAILY -> "Every day"
    HabitFrequency.WEEKDAYS -> "Weekdays"
    HabitFrequency.WEEKENDS -> "Weekends"
    HabitFrequency.SPECIFIC_DAYS -> "Specific days"
    HabitFrequency.TIMES_PER_WEEK -> "Times per week"
}

private fun TrackingType.displayName(): String = when (this) {
    TrackingType.BINARY -> "Yes/No"
    TrackingType.QUANTITATIVE -> "Quantity"
    TrackingType.DURATION -> "Duration"
    TrackingType.LOCATION -> "Location"
    TrackingType.FINANCIAL -> "Financial"
}
