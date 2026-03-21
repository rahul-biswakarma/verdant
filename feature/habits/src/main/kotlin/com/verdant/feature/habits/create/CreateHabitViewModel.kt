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

data class CreateHabitUiState(
    val aiInput: String = "",
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val draft: HabitDraft? = null,
    /** Which collapsible sections are expanded. */
    val trackingExpanded: Boolean = false,
    val reminderExpanded: Boolean = false,
    val moreOptionsExpanded: Boolean = false,
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

    // ── AI input ────────────────────────────────────────────────────────────

    fun onAiInputChange(text: String) {
        _uiState.update { it.copy(aiInput = text, aiError = null) }
    }

    fun onSubmitAiInput() {
        val text = _uiState.value.aiInput.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null) }
            runCatching { verdantAI.parseHabitDescription(text) }
                .onSuccess { parsed ->
                    _uiState.update { it.copy(isAiLoading = false, draft = parsed.toDraft()) }
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

    // ── Template selection ───────────────────────────────────────────────────

    fun onTemplateSelected(template: HabitTemplate) {
        _uiState.update { it.copy(draft = template.toDraft()) }
    }

    // ── Draft editing ────────────────────────────────────────────────────────

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

    // ── Section toggles ──────────────────────────────────────────────────────

    fun onToggleTracking() = _uiState.update { it.copy(trackingExpanded = !it.trackingExpanded) }
    fun onToggleReminder() = _uiState.update { it.copy(reminderExpanded = !it.reminderExpanded) }
    fun onToggleMoreOptions() = _uiState.update { it.copy(moreOptionsExpanded = !it.moreOptionsExpanded) }

    /** Ensure a draft always exists for the form-first approach. */
    fun ensureDraft() {
        if (_uiState.value.draft == null) {
            _uiState.update { it.copy(draft = HabitDraft()) }
        }
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    fun onSaveHabit() {
        val draft = _uiState.value.draft ?: return
        if (draft.name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                habitRepository.insert(draft.toHabit())
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

    private fun updateDraft(transform: (HabitDraft) -> HabitDraft) {
        _uiState.update { state ->
            state.copy(draft = state.draft?.let(transform))
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
    sortOrder = 0,
    createdAt = System.currentTimeMillis(),
)
