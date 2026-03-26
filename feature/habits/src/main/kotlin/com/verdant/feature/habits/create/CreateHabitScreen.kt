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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.X

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateHabitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onNavigateBack()
    }

    val phase = uiState.phase
    val isEntry = phase is CreateHabitPhase.ConversationalEntry

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ─────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Text(
                        text = if (isEntry) "New Habit" else "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (isEntry) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(TablerIcons.X, contentDescription = "Close")
                        }
                    } else {
                        IconButton(onClick = viewModel::onStartOver) {
                            Icon(TablerIcons.ArrowLeft, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )

            // ── Phase content ───────────────────────────────────────────
            AnimatedContent(
                targetState = phase::class,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 })
                        .togetherWith(fadeOut() + slideOutVertically { -it / 4 })
                },
                label = "phase_transition",
            ) { phaseClass ->
                when {
                    phaseClass == CreateHabitPhase.ConversationalEntry::class -> {
                        val entry = phase as? CreateHabitPhase.ConversationalEntry
                            ?: CreateHabitPhase.ConversationalEntry()
                        ConversationalEntrySection(
                            inputText = entry.inputText,
                            isLoading = uiState.isAiLoading,
                            error = uiState.aiError,
                            onInputChange = viewModel::onAiInputChange,
                            onSubmit = {
                                keyboardController?.hide()
                                viewModel.onSubmitAiInput()
                            },
                            onTemplateSelected = viewModel::onTemplateSelected,
                            onCreateManually = viewModel::onCreateManually,
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                        )
                    }

                    phaseClass == CreateHabitPhase.VisualizationPicker::class -> {
                        val picker = phase as? CreateHabitPhase.VisualizationPicker
                            ?: return@AnimatedContent
                        PreviewPhaseContent(
                            draft = picker.draft,
                            tweakExpanded = picker.tweakExpanded,
                            reminderExpanded = picker.reminderExpanded,
                            moreOptionsExpanded = picker.moreOptionsExpanded,
                            selectedVisualization = picker.selectedVisualization,
                            viewModel = viewModel,
                        )
                    }
                }
            }

            // ── Bottom button ───────────────────────────────────────────
            AnimatedVisibility(
                visible = !isEntry,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val draft = (phase as? CreateHabitPhase.VisualizationPicker)?.draft
                val habitColor = draft?.let { Color(it.color) }
                    ?: MaterialTheme.colorScheme.primary

                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = viewModel::onSaveHabit,
                        enabled = draft?.name?.isNotBlank() == true && !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = habitColor,
                            contentColor = if (habitColor.luminance() > 0.4f) Color.Black
                            else Color.White,
                        ),
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = if (habitColor.luminance() > 0.4f) Color.Black
                                else Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                "Start tracking",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPhaseContent(
    draft: HabitDraft,
    tweakExpanded: Boolean,
    reminderExpanded: Boolean,
    moreOptionsExpanded: Boolean,
    selectedVisualization: com.verdant.core.model.VisualizationType,
    viewModel: CreateHabitViewModel,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Preview card
        HabitPreviewCard(
            draft = draft,
            onTweakDetails = viewModel::onToggleTweakDetails,
            tweakExpanded = tweakExpanded,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // Unified tracking + visualization picker (always visible)
        TrackingVisualizationPicker(
            selectedTrackingType = draft.trackingType,
            selectedVisualization = selectedVisualization,
            habitColor = Color(draft.color),
            unit = draft.unit,
            targetValue = draft.targetValue,
            onOptionSelected = viewModel::onTrackingVisualizationSelected,
            onUnitChange = viewModel::onDraftUnitChange,
            onTargetChange = viewModel::onDraftTargetValueChange,
        )

        // Tweak details (progressive disclosure)
        AnimatedVisibility(
            visible = tweakExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionHeader(title = "What's your habit?")

                HabitNameField(
                    name = draft.name,
                    onNameChange = viewModel::onDraftNameChange,
                )

                IconColorRow(
                    selectedIcon = draft.icon,
                    selectedColor = draft.color,
                    onIconChange = viewModel::onDraftIconChange,
                    onColorChange = viewModel::onDraftColorChange,
                )

                SectionHeader(title = "When?")

                FrequencySelector(
                    frequency = draft.frequency,
                    scheduleDays = draft.scheduleDays,
                    onFrequencyChange = viewModel::onDraftFrequencyChange,
                    onScheduleDaysChange = viewModel::onDraftScheduleDaysChange,
                )

                CollapsibleSection(
                    title = "Remind me",
                    subtitle = if (draft.reminderEnabled) {
                        draft.reminderTimes.joinToString(" & ")
                    } else "Off",
                    expanded = reminderExpanded,
                    onToggle = viewModel::onToggleReminder,
                ) {
                    ReminderSection(
                        enabled = draft.reminderEnabled,
                        times = draft.reminderTimes,
                        onEnabledChange = viewModel::onDraftReminderEnabledChange,
                        onTimesChange = viewModel::onDraftReminderTimesChange,
                    )
                }

                CollapsibleSection(
                    title = "More options",
                    subtitle = listOfNotNull(
                        draft.description.takeIf { it.isNotBlank() }?.let { "Notes" },
                        draft.label.takeIf { it.isNotBlank() },
                        draft.streakGoal?.let { "Goal: $it days" },
                    ).joinToString(" \u00B7 ").ifBlank { "Description, label, streak goal" },
                    expanded = moreOptionsExpanded,
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
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
