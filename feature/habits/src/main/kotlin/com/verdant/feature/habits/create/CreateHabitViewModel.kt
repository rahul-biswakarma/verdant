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
import com.verdant.core.model.defaultVisualization
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

/** Total steps in the creation wizard (0-indexed: 0..TOTAL_STEPS-1). */
const val WIZARD_TOTAL_STEPS = 4

data class CreateHabitUiState(
    val currentStep: Int = 0,
    val aiInput: String = "",
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val draft: HabitDraft? = null,
    /** Suggested TrackingType based on keyword detection from the name or AI input. */
    val suggestedType: TrackingType? = null,
    /** Which collapsible sections are expanded (used in details step). */
    val reminderExpanded: Boolean = false,
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

    // ── Wizard navigation ────────────────────────────────────────────────────

    fun onNextStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(WIZARD_TOTAL_STEPS - 1)) }
    }

    fun onPrevStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun onGoToStep(step: Int) {
        _uiState.update { it.copy(currentStep = step.coerceIn(0, WIZARD_TOTAL_STEPS - 1)) }
    }

    // ── AI input ────────────────────────────────────────────────────────────

    fun onAiInputChange(text: String) {
        _uiState.update { it.copy(aiInput = text, aiError = null, suggestedType = detectType(text)) }
    }

    fun onSubmitAiInput() {
        val text = _uiState.value.aiInput.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null) }
            runCatching { verdantAI.parseHabitDescription(text) }
                .onSuccess { parsed ->
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            draft = parsed.toDraft(),
                            suggestedType = parsed.trackingType,
                            currentStep = 1, // jump to type confirmation
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

    // ── Template selection ───────────────────────────────────────────────────

    fun onTemplateSelected(template: HabitTemplate) {
        _uiState.update {
            it.copy(
                draft = template.toDraft(),
                suggestedType = template.trackingType,
                currentStep = 3, // jump straight to visualization (type + details already decided)
            )
        }
    }

    // ── Draft editing ────────────────────────────────────────────────────────

    fun onDraftNameChange(name: String) {
        updateDraft { it.copy(name = name) }
        _uiState.update { it.copy(suggestedType = detectType(name).takeIf { _ -> it.suggestedType == null }) }
    }
    fun onDraftDescriptionChange(desc: String) = updateDraft { it.copy(description = desc) }
    fun onDraftIconChange(icon: String) = updateDraft { it.copy(icon = icon) }
    fun onDraftColorChange(color: Long) = updateDraft { it.copy(color = color) }
    fun onDraftLabelChange(label: String) = updateDraft { it.copy(label = label) }
    fun onDraftTrackingTypeChange(type: TrackingType) = updateDraft {
        it.copy(
            trackingType = type,
            // Auto-update visualization to match the new type's default if user hasn't changed it
            visualizationType = type.defaultVisualization(),
        )
    }
    fun onDraftVisualizationTypeChange(type: VisualizationType) = updateDraft { it.copy(visualizationType = type) }
    fun onDraftUnitChange(unit: String) = updateDraft { it.copy(unit = unit) }
    fun onDraftTargetValueChange(value: Double?) = updateDraft { it.copy(targetValue = value) }
    fun onDraftFrequencyChange(freq: HabitFrequency) = updateDraft { it.copy(frequency = freq) }
    fun onDraftScheduleDaysChange(days: Int) = updateDraft { it.copy(scheduleDays = days) }
    fun onDraftReminderEnabledChange(enabled: Boolean) = updateDraft { it.copy(reminderEnabled = enabled) }
    fun onDraftReminderTimesChange(times: List<String>) = updateDraft { it.copy(reminderTimes = times) }
    fun onDraftReminderDaysChange(days: Int) = updateDraft { it.copy(reminderDays = days) }
    fun onDraftStreakGoalChange(goal: Int?) = updateDraft { it.copy(streakGoal = goal) }

    fun onCheckpointStepChange(index: Int, value: String) = updateDraft {
        val updated = it.checkpointSteps.toMutableList().also { list -> list[index] = value }
        it.copy(checkpointSteps = updated)
    }
    fun onCheckpointStepAdd() = updateDraft { it.copy(checkpointSteps = it.checkpointSteps + "") }
    fun onCheckpointStepRemove(index: Int) = updateDraft {
        it.copy(checkpointSteps = it.checkpointSteps.toMutableList().also { list -> list.removeAt(index) })
    }

    fun onToggleReminder() = _uiState.update { it.copy(reminderExpanded = !it.reminderExpanded) }

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

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun updateDraft(transform: (HabitDraft) -> HabitDraft) {
        _uiState.update { state ->
            state.copy(draft = state.draft?.let(transform))
        }
    }

    /**
     * Keyword-based habit type detection from user input.
     * Returns null when the text is too short or ambiguous.
     */
    private fun detectType(text: String): TrackingType? {
        if (text.length < 3) return null
        val lower = text.lowercase()
        return when {
            lower.containsAny(
                "build", "assemble", "learn to", "complete", "project",
                "drone", "phase", "milestone", "ship", "launch", "course",
            ) -> TrackingType.CHECKPOINT
            lower.containsAny(
                " km", " km/", "kilometers", "miles", "glasses", "pages",
                "reps", "bottles", "cups", "calories", " steps", "litres",
                "liters", "pushup", "pull-up",
            ) -> TrackingType.QUANTITATIVE
            lower.containsAny(
                "meditat", "workout", "practice ", "study", "code",
                "focus", "session", "deep work", "writing session", "piano",
                "guitar", "exercise", "train", "gym",
            ) -> TrackingType.DURATION
            lower.containsAny(
                "take ", "drink ", "eat ", "avoid", "no ", "sleep", "quit",
                "wake up", "daily ", "every day", "supplement", "vitamin",
                "pill", "journal", "floss",
            ) -> TrackingType.BINARY
            else -> null
        }
    }
}

private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }

private fun HabitDraft.toHabit() = Habit(
    id = UUID.randomUUID().toString(),
    name = name.trim(),
    description = description.trim(),
    icon = icon,
    color = color,
    label = label.takeIf { it.isNotBlank() },
    trackingType = trackingType,
    visualizationType = visualizationType,
    unit = unit.takeIf { it.isNotBlank() },
    targetValue = targetValue,
    checkpointSteps = checkpointSteps.filter { it.isNotBlank() },
    frequency = frequency,
    scheduleDays = scheduleDays,
    isArchived = false,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTimeJoined.takeIf { reminderEnabled },
    reminderDays = if (reminderEnabled) reminderDays else 0,
    sortOrder = 0,
    createdAt = System.currentTimeMillis(),
)
