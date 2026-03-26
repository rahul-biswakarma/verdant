package com.verdant.feature.habits.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.ai.VerdantAI
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.repository.LabelRepository
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.Label
import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ── Phase-based state machine ────────────────────────────────────────────────

sealed interface CreateHabitPhase {
    /** Phase 1: Large text area + mic + templates. No draft yet. */
    data class ConversationalEntry(
        val inputText: String = "",
    ) : CreateHabitPhase

    /** Phase 2: Preview card + unified tracking/visualization picker. */
    data class VisualizationPicker(
        val draft: HabitDraft,
        val selectedVisualization: VisualizationType,
        val suggestedVisualization: VisualizationType,
        val tweakExpanded: Boolean = false,
        val reminderExpanded: Boolean = false,
        val moreOptionsExpanded: Boolean = false,
    ) : CreateHabitPhase
}

data class CreateHabitUiState(
    val phase: CreateHabitPhase = CreateHabitPhase.ConversationalEntry(),
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class CreateHabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val labelRepository: LabelRepository,
    private val verdantAI: VerdantAI,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateHabitUiState())
    val uiState: StateFlow<CreateHabitUiState> = _uiState.asStateFlow()

    val labels: StateFlow<List<Label>> = labelRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Phase 1: Conversational Entry ─────────────────────────────────────

    fun onAiInputChange(text: String) {
        val phase = _uiState.value.phase
        if (phase is CreateHabitPhase.ConversationalEntry) {
            _uiState.update {
                it.copy(
                    phase = phase.copy(inputText = text),
                    aiError = null,
                )
            }
        }
    }

    fun onSubmitAiInput() {
        val phase = _uiState.value.phase as? CreateHabitPhase.ConversationalEntry ?: return
        val text = phase.inputText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null) }
            runCatching { verdantAI.parseHabitDescription(text) }
                .onSuccess { parsed ->
                    val draft = parsed.toDraft()
                    val suggested = suggestVisualization(draft.trackingType)
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            phase = CreateHabitPhase.VisualizationPicker(
                                draft = draft,
                                selectedVisualization = suggested,
                                suggestedVisualization = suggested,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            aiError = error.message ?: "Failed to parse habit description",
                        )
                    }
                }
        }
    }

    fun onTemplateSelected(template: HabitTemplate) {
        val draft = template.toDraft()
        val suggested = suggestVisualization(draft.trackingType)
        _uiState.update {
            it.copy(
                phase = CreateHabitPhase.VisualizationPicker(
                    draft = draft,
                    selectedVisualization = suggested,
                    suggestedVisualization = suggested,
                ),
            )
        }
    }

    fun onCreateManually() {
        val draft = HabitDraft()
        val suggested = suggestVisualization(draft.trackingType)
        _uiState.update {
            it.copy(
                phase = CreateHabitPhase.VisualizationPicker(
                    draft = draft,
                    selectedVisualization = suggested,
                    suggestedVisualization = suggested,
                    tweakExpanded = true,
                ),
            )
        }
    }

    fun onStartOver() {
        _uiState.update {
            it.copy(
                phase = CreateHabitPhase.ConversationalEntry(),
                aiError = null,
            )
        }
    }

    // ── Phase 2/3: Draft editing ──────────────────────────────────────────

    fun onDraftNameChange(name: String) = updateDraft { it.copy(name = name) }
    fun onDraftDescriptionChange(desc: String) = updateDraft { it.copy(description = desc) }
    fun onDraftIconChange(icon: String) = updateDraft { it.copy(icon = icon) }
    fun onDraftColorChange(color: Long) = updateDraft { it.copy(color = color) }
    fun onDraftLabelChange(label: String) = updateDraft { it.copy(label = label) }
    fun onDraftTrackingTypeChange(type: TrackingType) = updateDraft { it.copy(trackingType = type) }
    fun onDraftUnitChange(unit: String) = updateDraft { it.copy(unit = unit) }
    fun onDraftTargetValueChange(value: Double?) = updateDraft { it.copy(targetValue = value) }
    fun onDraftFrequencyChange(freq: HabitFrequency) = updateDraft { it.copy(frequency = freq) }
    fun onDraftScheduleDaysChange(days: Int) = updateDraft { it.copy(scheduleDays = days) }
    fun onDraftReminderEnabledChange(enabled: Boolean) = updateDraft { it.copy(reminderEnabled = enabled) }
    fun onDraftReminderTimesChange(times: List<String>) = updateDraft { it.copy(reminderTimes = times) }
    fun onDraftReminderDaysChange(days: Int) = updateDraft { it.copy(reminderDays = days) }
    fun onDraftStreakGoalChange(goal: Int?) = updateDraft { it.copy(streakGoal = goal) }

    // ── Section toggles ──────────────────────────────────────────────────

    fun onToggleTweakDetails() {
        _uiState.update { state ->
            val phase = state.phase
            if (phase is CreateHabitPhase.VisualizationPicker) {
                state.copy(phase = phase.copy(tweakExpanded = !phase.tweakExpanded))
            } else state
        }
    }

    fun onToggleReminder() {
        _uiState.update { state ->
            val phase = state.phase
            if (phase is CreateHabitPhase.VisualizationPicker) {
                state.copy(phase = phase.copy(reminderExpanded = !phase.reminderExpanded))
            } else state
        }
    }

    fun onToggleMoreOptions() {
        _uiState.update { state ->
            val phase = state.phase
            if (phase is CreateHabitPhase.VisualizationPicker) {
                state.copy(phase = phase.copy(moreOptionsExpanded = !phase.moreOptionsExpanded))
            } else state
        }
    }

    // ── Tracking + Visualization selection ─────────────────────────────────

    fun onTrackingVisualizationSelected(trackingType: TrackingType, vizType: VisualizationType) {
        _uiState.update { state ->
            val phase = state.phase
            if (phase is CreateHabitPhase.VisualizationPicker) {
                state.copy(
                    phase = phase.copy(
                        draft = phase.draft.copy(trackingType = trackingType),
                        selectedVisualization = vizType,
                    ),
                )
            } else state
        }
    }

    fun onVisualizationSelected(type: VisualizationType) {
        _uiState.update { state ->
            val phase = state.phase
            if (phase is CreateHabitPhase.VisualizationPicker) {
                state.copy(phase = phase.copy(selectedVisualization = type))
            } else state
        }
    }

    // ── Save ─────────────────────────────────────────────────────────────

    fun onSaveHabit() {
        val draft = currentDraft() ?: return
        if (draft.name.isBlank()) return

        val vizType = when (val phase = _uiState.value.phase) {
            is CreateHabitPhase.VisualizationPicker -> phase.selectedVisualization
            else -> VisualizationType.CONTRIBUTION_GRID
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                habitRepository.insert(draft.copy(visualizationType = vizType).toHabit())
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            }.onFailure {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun onCreateNewLabel(name: String, color: Long) {
        viewModelScope.launch {
            val label = Label(id = UUID.randomUUID().toString(), name = name, color = color)
            labelRepository.insert(label)
            updateDraft { it.copy(label = name) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun currentDraft(): HabitDraft? = when (val phase = _uiState.value.phase) {
        is CreateHabitPhase.VisualizationPicker -> phase.draft
        is CreateHabitPhase.ConversationalEntry -> null
    }

    private fun updateDraft(transform: (HabitDraft) -> HabitDraft) {
        _uiState.update { state ->
            val phase = state.phase
            if (phase is CreateHabitPhase.VisualizationPicker) {
                state.copy(phase = phase.copy(draft = transform(phase.draft)))
            } else state
        }
    }
}

private fun HabitDraft.toHabit() = Habit(
    id = UUID.randomUUID().toString(),
    name = name.trim(),
    description = description.trim(),
    icon = icon,
    color = color,
    label = label.takeIf { it.isNotBlank() },
    trackingType = trackingType,
    unit = unit.takeIf { it.isNotBlank() },
    targetValue = targetValue,
    frequency = frequency,
    scheduleDays = scheduleDays,
    isArchived = false,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTimeJoined.takeIf { reminderEnabled },
    reminderDays = if (reminderEnabled) reminderDays else 0,
    visualizationType = visualizationType,
    sortOrder = 0,
    createdAt = System.currentTimeMillis(),
)
