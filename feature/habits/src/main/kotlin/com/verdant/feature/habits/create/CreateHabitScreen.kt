package com.verdant.feature.habits.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.ArrowRight
import compose.icons.tablericons.Check
import compose.icons.tablericons.ChevronDown
import compose.icons.tablericons.ChevronUp
import compose.icons.tablericons.Microphone
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Rocket
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType
import com.verdant.core.model.recommendedFor

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateHabitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.ensureDraft() }
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onNavigateBack()
    }

    val draft = uiState.draft ?: return
    val habitColor = Color(draft.color)
    val keyboardController = LocalSoftwareKeyboardController.current

    // Progress (steps 1-3 have progress; step 0 intro has no progress bar)
    val progressFraction = if (uiState.currentStep == 0) 0f
                           else uiState.currentStep.toFloat() / (WIZARD_TOTAL_STEPS - 1).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "progress",
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ─────────────────────────────────────────────────────
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = uiState.currentStep,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) using SizeTransform(clip = false) },
                        label = "stepTitle",
                    ) { step ->
                        Text(
                            text = when (step) {
                                0 -> "New Habit"
                                1 -> "Track how?"
                                2 -> "The details"
                                3 -> "When?"
                                else -> "Look & feel"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.currentStep == 0) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(TablerIcons.X, contentDescription = "Close")
                        }
                    } else {
                        IconButton(onClick = viewModel::onPrevStep) {
                            Icon(TablerIcons.ArrowLeft, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (uiState.currentStep == 0) {
                        IconButton(onClick = { /* TODO: voice input */ }) {
                            Icon(TablerIcons.Microphone, contentDescription = "Voice input")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            // ── Progress bar (hidden on intro step) ─────────────────────────
            AnimatedVisibility(visible = uiState.currentStep > 0) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = habitColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Spacer(Modifier.height(if (uiState.currentStep > 0) 8.dp else 0.dp))

            // ── Step content (animated slide) ────────────────────────────────
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 2 } + fadeIn(tween(260)) togetherWith
                            slideOutHorizontally { -it / 2 } + fadeOut(tween(180)) using
                            SizeTransform(clip = false)
                    } else {
                        slideInHorizontally { -it / 2 } + fadeIn(tween(260)) togetherWith
                            slideOutHorizontally { it / 2 } + fadeOut(tween(180)) using
                            SizeTransform(clip = false)
                    }
                },
                modifier = Modifier.weight(1f),
                label = "stepContent",
            ) { step ->
                when (step) {
                    0 -> IntroStep(
                        aiInput = uiState.aiInput,
                        isAiLoading = uiState.isAiLoading,
                        aiError = uiState.aiError,
                        onAiInputChange = viewModel::onAiInputChange,
                        onSubmitAi = {
                            keyboardController?.hide()
                            viewModel.onSubmitAiInput()
                        },
                        onTemplateSelected = viewModel::onTemplateSelected,
                        onStartFresh = {
                            viewModel.ensureDraft()
                            viewModel.onNextStep()
                        },
                    )
                    1 -> TypeStep(
                        selectedType = draft.trackingType,
                        suggestedType = uiState.suggestedType,
                        onTypeSelected = { type ->
                            viewModel.onDraftTrackingTypeChange(type)
                            viewModel.onNextStep()
                        },
                    )
                    2 -> DetailsStep(
                        draft = draft,
                        onNameChange = viewModel::onDraftNameChange,
                        onIconChange = viewModel::onDraftIconChange,
                        onColorChange = viewModel::onDraftColorChange,
                        onUnitChange = viewModel::onDraftUnitChange,
                        onTargetChange = viewModel::onDraftTargetValueChange,
                        onCheckpointStepChange = viewModel::onCheckpointStepChange,
                        onCheckpointStepAdd = viewModel::onCheckpointStepAdd,
                        onCheckpointStepRemove = viewModel::onCheckpointStepRemove,
                        onDescriptionChange = viewModel::onDraftDescriptionChange,
                        onLabelChange = viewModel::onDraftLabelChange,
                    )
                    3 -> ScheduleStep(
                        draft = draft,
                        reminderExpanded = uiState.reminderExpanded,
                        onFrequencyChange = viewModel::onDraftFrequencyChange,
                        onScheduleDaysChange = viewModel::onDraftScheduleDaysChange,
                        onToggleReminder = viewModel::onToggleReminder,
                        onReminderEnabledChange = viewModel::onDraftReminderEnabledChange,
                        onReminderTimesChange = viewModel::onDraftReminderTimesChange,
                    )
                    else -> VisualizationStep(
                        selectedType = draft.trackingType,
                        selectedViz = draft.visualizationType,
                        onVizSelected = viewModel::onDraftVisualizationTypeChange,
                    )
                }
            }

            // ── Bottom nav bar ───────────────────────────────────────────────
            BottomNavBar(
                step = uiState.currentStep,
                draft = draft,
                isSaving = uiState.isSaving,
                habitColor = habitColor,
                onNext = viewModel::onNextStep,
                onSave = viewModel::onSaveHabit,
            )
        }
    }
}

// ── Step 0: Intro ────────────────────────────────────────────────────────────

@Composable
private fun IntroStep(
    aiInput: String,
    isAiLoading: Boolean,
    aiError: String?,
    onAiInputChange: (String) -> Unit,
    onSubmitAi: () -> Unit,
    onTemplateSelected: (HabitTemplate) -> Unit,
    onStartFresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Hero headline
        Text(
            text = "What habit will\nyou build?",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            lineHeight = 40.sp,
        )

        // AI describe card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            ),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        TablerIcons.Stars,
                        null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        "Describe it — we'll set it up",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = aiInput,
                        onValueChange = onAiInputChange,
                        placeholder = {
                            Text(
                                "e.g., Cycle 20 km every morning",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSubmitAi() }),
                    )
                    if (isAiLoading) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                                .clickable(onClick = onSubmitAi),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                TablerIcons.ArrowRight,
                                null,
                                Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondary,
                            )
                        }
                    }
                }
                if (aiError != null) {
                    Text(
                        aiError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // Divider with label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            Text(
                "or pick a template",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }

        // Quick templates in horizontal scroll
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            popularTemplates.forEach { template ->
                TemplateChip(template = template, onClick = { onTemplateSelected(template) })
            }
        }

        // Start fresh button
        TextButton(
            onClick = onStartFresh,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                "Start from scratch",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                TablerIcons.ArrowRight,
                null,
                Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TemplateChip(template: HabitTemplate, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(template.color).copy(alpha = 0.12f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(template.icon, fontSize = 18.sp)
            Text(
                template.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Step 1: Type Selection ───────────────────────────────────────────────────

private data class TypeOption(
    val type: TrackingType,
    val emoji: String,
    val name: String,
    val tagline: String,
    val example: String,
    val color: Long,
)

private val typeOptions = listOf(
    TypeOption(
        TrackingType.BINARY, "✅", "Binary",
        "Yes or no — every day counts",
        "Take vitamins · Sleep on time · No sugar",
        0xFF5A7A60L,
    ),
    TypeOption(
        TrackingType.CHECKPOINT, "🗺️", "Checkpoint",
        "Step-by-step milestones",
        "Build a drone · Write a book · Learn piano",
        0xFF5A6B7AL,
    ),
    TypeOption(
        TrackingType.DURATION, "⏱️", "Duration",
        "Time invested, intensity tracked",
        "Deep focus · Meditation · Gym session",
        0xFF8B7355L,
    ),
    TypeOption(
        TrackingType.QUANTITATIVE, "📊", "Volume",
        "Cumulative numbers add up",
        "Cycling km · Pages read · Glasses of water",
        0xFFE8673CL,
    ),
)

@Composable
private fun TypeStep(
    selectedType: TrackingType,
    suggestedType: TrackingType?,
    onTypeSelected: (TrackingType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "How will you\ntrack it?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp,
        )
        Text(
            "Choose the tracking style that fits your habit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        typeOptions.forEach { option ->
            TypeCard(
                option = option,
                isSelected = selectedType == option.type,
                isSuggested = suggestedType == option.type,
                onClick = { onTypeSelected(option.type) },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TypeCard(
    option: TypeOption,
    isSelected: Boolean,
    isSuggested: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = Color(option.color)
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "typeBg",
    )
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(option.emoji, fontSize = 26.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        option.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (isSuggested) {
                        SuggestedBadge()
                    }
                }
                Text(
                    option.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    option.example,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(alpha = 0.8f),
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(TablerIcons.Check, null, Modifier.size(14.dp), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SuggestedBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Suggested",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Step 2: Details ──────────────────────────────────────────────────────────

@Composable
private fun DetailsStep(
    draft: HabitDraft,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    onUnitChange: (String) -> Unit,
    onTargetChange: (Double?) -> Unit,
    onCheckpointStepChange: (Int, String) -> Unit,
    onCheckpointStepAdd: () -> Unit,
    onCheckpointStepRemove: (Int) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Name & customize",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        // Icon + name row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconPickerButton(
                selectedIcon = draft.icon,
                selectedColor = draft.color,
                onIconChange = onIconChange,
            )
            OutlinedTextField(
                value = draft.name,
                onValueChange = onNameChange,
                placeholder = {
                    Text(
                        when (draft.trackingType) {
                            TrackingType.BINARY -> "e.g., Take medicine, Journal…"
                            TrackingType.CHECKPOINT -> "e.g., Build a Drone, Write a Book…"
                            TrackingType.DURATION -> "e.g., Deep Focus, Morning Run…"
                            TrackingType.QUANTITATIVE -> "e.g., Cycling, Drink Water…"
                            else -> "e.g., Take medicine, Read…"
                        }
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )
        }

        // Color picker
        ColorPicker(selectedColor = draft.color, onColorChange = onColorChange)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Type-specific fields
        when (draft.trackingType) {
            TrackingType.QUANTITATIVE -> {
                Text(
                    "Set your target",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draft.targetValue?.fmt() ?: "",
                        onValueChange = { onTargetChange(it.toDoubleOrNull()) },
                        label = { Text("Target") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = draft.unit,
                        onValueChange = onUnitChange,
                        label = { Text("Unit") },
                        placeholder = { Text("km, glasses, pages") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                    )
                }
            }
            TrackingType.DURATION -> {
                Text(
                    "Session target",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = draft.targetValue?.fmt() ?: "",
                    onValueChange = { onTargetChange(it.toDoubleOrNull()) },
                    label = { Text("Target duration (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            TrackingType.FINANCIAL -> {
                Text(
                    "Budget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = draft.targetValue?.fmt() ?: "",
                    onValueChange = { onTargetChange(it.toDoubleOrNull()) },
                    label = { Text("Daily budget") },
                    prefix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            TrackingType.CHECKPOINT -> {
                Text(
                    "Milestones",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "List the steps you need to complete, in order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CheckpointStepEditor(
                    steps = draft.checkpointSteps,
                    habitColor = Color(draft.color),
                    onStepChange = onCheckpointStepChange,
                    onStepAdd = onCheckpointStepAdd,
                    onStepRemove = onCheckpointStepRemove,
                )
            }
            else -> {} // BINARY: no extra fields needed
        }

        // Optional label
        OutlinedTextField(
            value = draft.label,
            onValueChange = onLabelChange,
            label = { Text("Category (optional)") },
            placeholder = { Text("Health, Fitness, Work…") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPickerButton(
    selectedIcon: String,
    selectedColor: Long,
    onIconChange: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(selectedColor).copy(alpha = 0.18f))
                .border(
                    1.5.dp,
                    Color(selectedColor).copy(alpha = 0.35f),
                    RoundedCornerShape(16.dp),
                )
                .clickable { showPicker = !showPicker },
            contentAlignment = Alignment.Center,
        ) {
            Text(selectedIcon, fontSize = 28.sp)
        }
        AnimatedVisibility(visible = showPicker, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            FlowRow(
                modifier = Modifier.width(200.dp).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ICON_PRESETS.forEach { icon ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedIcon == icon) Color(selectedColor).copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { onIconChange(icon); showPicker = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(icon, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(selectedColor: Long, onColorChange: (Long) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        COLOR_PRESETS.forEach { colorLong ->
            val isSelected = selectedColor == colorLong
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(colorLong))
                    .then(
                        if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier
                    )
                    .clickable { onColorChange(colorLong) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Icon(TablerIcons.Check, null, Modifier.size(14.dp), tint = Color.White)
            }
        }
    }
}

@Composable
private fun CheckpointStepEditor(
    steps: List<String>,
    habitColor: Color,
    onStepChange: (Int, String) -> Unit,
    onStepAdd: () -> Unit,
    onStepRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Step number badge
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (step.isNotBlank()) habitColor.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (step.isNotBlank()) habitColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = step,
                    onValueChange = { onStepChange(index, it) },
                    placeholder = { Text("Milestone ${index + 1}") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                )
                if (steps.size > 1) {
                    IconButton(onClick = { onStepRemove(index) }, modifier = Modifier.size(32.dp)) {
                        Icon(TablerIcons.X, "Remove step", Modifier.size(16.dp))
                    }
                }
            }
        }
        TextButton(onClick = onStepAdd) {
            Icon(TablerIcons.Plus, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add milestone")
        }
    }
}

// ── Step 3: Schedule ─────────────────────────────────────────────────────────

@Composable
private fun ScheduleStep(
    draft: HabitDraft,
    reminderExpanded: Boolean,
    onFrequencyChange: (HabitFrequency) -> Unit,
    onScheduleDaysChange: (Int) -> Unit,
    onToggleReminder: () -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimesChange: (List<String>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "When will you\ndo this?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp,
        )

        // Frequency chips
        val options = listOf(
            HabitFrequency.DAILY to "Daily",
            HabitFrequency.WEEKDAYS to "Weekdays",
            HabitFrequency.WEEKENDS to "Weekends",
            HabitFrequency.SPECIFIC_DAYS to "Custom",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (freq, label) ->
                FilterChip(
                    selected = draft.frequency == freq,
                    onClick = {
                        onFrequencyChange(freq)
                        when (freq) {
                            HabitFrequency.DAILY -> onScheduleDaysChange(0x7F)
                            HabitFrequency.WEEKDAYS -> onScheduleDaysChange(0x1F)
                            HabitFrequency.WEEKENDS -> onScheduleDaysChange(0x60)
                            else -> {}
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

        // Custom day picker
        AnimatedVisibility(visible = draft.frequency == HabitFrequency.SPECIFIC_DAYS) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SCHEDULE_DAYS.forEach { (dayLabel, bit) ->
                    val isActive = draft.scheduleDays and bit != 0
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                val newDays = if (isActive) draft.scheduleDays and bit.inv()
                                             else draft.scheduleDays or bit
                                onScheduleDaysChange(newDays)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Reminder toggle header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleReminder)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Remind me",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (draft.reminderEnabled) draft.reminderTimes.joinToString(" & ")
                    else "No reminder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = draft.reminderEnabled,
                    onCheckedChange = onReminderEnabledChange,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (reminderExpanded) TablerIcons.ChevronUp else TablerIcons.ChevronDown,
                    null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(
            visible = reminderExpanded && draft.reminderEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.reminderTimes.forEachIndexed { index, time ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = time,
                            onValueChange = { newTime ->
                                val updated = draft.reminderTimes.toMutableList()
                                updated[index] = newTime
                                onReminderTimesChange(updated)
                            },
                            label = { Text("Time ${index + 1}") },
                            placeholder = { Text("HH:mm") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )
                        if (draft.reminderTimes.size > 1) {
                            IconButton(
                                onClick = {
                                    val updated = draft.reminderTimes.toMutableList()
                                    updated.removeAt(index)
                                    onReminderTimesChange(updated)
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(TablerIcons.X, "Remove", Modifier.size(16.dp))
                            }
                        }
                    }
                }
                TextButton(onClick = { onReminderTimesChange(draft.reminderTimes + "08:00") }) {
                    Icon(TablerIcons.Plus, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add another time")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Step 4: Visualization Picker ─────────────────────────────────────────────

private data class VizOption(
    val type: VisualizationType,
    val emoji: String,
    val name: String,
    val description: String,
)

private val vizOptions = listOf(
    VizOption(
        VisualizationType.PIXEL_GRID, "🟩",
        "Pixel Art Grid",
        "Your streak immortalized as a growing mosaic — GitHub-style, but more satisfying.",
    ),
    VizOption(
        VisualizationType.TOPO_MAP, "🗻",
        "Topographic Map",
        "Your journey rendered as terrain — each milestone raises the mountain.",
    ),
    VizOption(
        VisualizationType.AUDIO_WAVEFORM, "🎵",
        "Audio Waveform",
        "Sessions pulse like a waveform — intensity and rhythm made visible.",
    ),
    VizOption(
        VisualizationType.PHYSICS_JAR, "🫙",
        "Physics Jar",
        "A liquid-filled jar that rises as you accumulate — fill it to the brim.",
    ),
    VizOption(
        VisualizationType.RPG_RADAR, "🕸️",
        "RPG Radar Chart",
        "Multi-dimensional radar chart — watch your stats grow like a character sheet.",
    ),
)

@Composable
private fun VisualizationStep(
    selectedType: TrackingType,
    selectedViz: VisualizationType,
    onVizSelected: (VisualizationType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "How should your\nprogress look?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp,
        )
        Text(
            "Pick a visualization style for your habit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        vizOptions.forEach { option ->
            val isRecommended = selectedType in option.type.recommendedFor()
            VizCard(
                option = option,
                isSelected = selectedViz == option.type,
                isRecommended = isRecommended,
                onClick = { onVizSelected(option.type) },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VizCard(
    option: VizOption,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit,
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                      else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "vizBg",
    )
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(option.emoji, fontSize = 26.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        option.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (isRecommended) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Text(
                                "Best fit",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Text(
                    option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(TablerIcons.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// ── Bottom Navigation Bar ────────────────────────────────────────────────────

@Composable
private fun BottomNavBar(
    step: Int,
    draft: HabitDraft,
    isSaving: Boolean,
    habitColor: Color,
    onNext: () -> Unit,
    onSave: () -> Unit,
) {
    // Don't show bottom bar on intro step (it has its own CTA)
    if (step == 0) return

    val isLastStep = step == WIZARD_TOTAL_STEPS - 1
    val canProceed = when (step) {
        2 -> draft.name.isNotBlank()
        else -> true
    }
    val contentColor = if (habitColor.luminance() > 0.4f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Button(
            onClick = if (isLastStep) onSave else onNext,
            enabled = canProceed && !isSaving,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = habitColor,
                contentColor = contentColor,
                disabledContainerColor = habitColor.copy(alpha = 0.4f),
            ),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else if (isLastStep) {
                Icon(TablerIcons.Rocket, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start tracking", style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    when (step) {
                        1 -> "Set details"
                        2 -> "Set schedule"
                        3 -> "Pick visualization"
                        else -> "Next"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(6.dp))
                Icon(TablerIcons.ArrowRight, null, Modifier.size(18.dp))
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun Double.fmt(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.1f".format(this)
