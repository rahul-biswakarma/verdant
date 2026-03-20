package com.verdant.feature.settings.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// scheduleDays bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
private const val ALL_DAYS = 0x7F
private const val WEEKDAYS = 0x1F

data class StarterTemplate(
    val name: String,
    val icon: String,
    val color: Long,
    val label: String,
    val trackingType: TrackingType,
    val unit: String? = null,
    val targetValue: Double? = null,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val scheduleDays: Int = ALL_DAYS,
    val reminderTime: String? = null,
    val category: String,
)

data class OnboardingDraft(
    val template: StarterTemplate,
    val displayName: String = template.name,
    val icon: String = template.icon,
    val color: Long = template.color,
)

data class OnboardingUiState(
    val selectedTemplates: Set<StarterTemplate> = emptySet(),
    val drafts: List<OnboardingDraft> = emptyList(),
    val morningMotivation: Boolean = true,
    val habitReminders: Boolean = true,
    val isSaving: Boolean = false,
)

val starterTemplates: List<StarterTemplate> = listOf(
    StarterTemplate("Take vitamins", "💊", 0xFF30A14EL, "Health", TrackingType.BINARY, category = "Health", reminderTime = "08:00"),
    StarterTemplate("Drink water", "💧", 0xFF2196F3L, "Health", TrackingType.QUANTITATIVE, "glasses", 8.0, category = "Health"),
    StarterTemplate("Meditate", "🧘", 0xFF9C27B0L, "Health", TrackingType.DURATION, "min", 10.0, category = "Health", reminderTime = "07:00"),
    StarterTemplate("Sleep by 11pm", "😴", 0xFF3F51B5L, "Health", TrackingType.BINARY, category = "Health", reminderTime = "22:00"),
    StarterTemplate("No junk food", "🥗", 0xFF4CAF50L, "Health", TrackingType.BINARY, category = "Health"),
    StarterTemplate("Cycling", "🚴", 0xFFFF9800L, "Fitness", TrackingType.DURATION, "min", 30.0, category = "Fitness", reminderTime = "07:00"),
    StarterTemplate("Running", "🏃", 0xFFFF5722L, "Fitness", TrackingType.QUANTITATIVE, "km", 5.0, category = "Fitness", reminderTime = "06:30"),
    StarterTemplate("Walk 10k steps", "👟", 0xFF8BC34AL, "Fitness", TrackingType.QUANTITATIVE, "steps", 10000.0, category = "Fitness"),
    StarterTemplate("Gym workout", "🏋️", 0xFFE91E63L, "Fitness", TrackingType.DURATION, "min", 60.0, category = "Fitness", reminderTime = "07:00"),
    StarterTemplate("Read books", "📚", 0xFF00BCD4L, "Learning", TrackingType.QUANTITATIVE, "pages", 20.0, category = "Learning", reminderTime = "21:00"),
    StarterTemplate("Practice coding", "💻", 0xFF607D8BL, "Learning", TrackingType.DURATION, "min", 30.0, scheduleDays = WEEKDAYS, category = "Learning", reminderTime = "20:00"),
    StarterTemplate("Learn language", "🗣️", 0xFF009688L, "Learning", TrackingType.DURATION, "min", 15.0, category = "Learning", reminderTime = "19:00"),
    StarterTemplate("Track spending", "💰", 0xFFFFEB3BL, "Finance", TrackingType.FINANCIAL, category = "Finance", reminderTime = "20:00"),
    StarterTemplate("No impulse buying", "🛒", 0xFFFF9800L, "Finance", TrackingType.BINARY, category = "Finance"),
    StarterTemplate("Journal", "📓", 0xFF795548L, "Lifestyle", TrackingType.BINARY, category = "Lifestyle", reminderTime = "21:30"),
    StarterTemplate("No social media", "📵", 0xFF607D8BL, "Lifestyle", TrackingType.BINARY, category = "Lifestyle"),
    StarterTemplate("Cook at home", "🍳", 0xFFFF8F00L, "Lifestyle", TrackingType.BINARY, category = "Lifestyle", reminderTime = "17:30"),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun toggleTemplate(template: StarterTemplate) {
        _uiState.update { state ->
            val newSelected = if (template in state.selectedTemplates) {
                state.selectedTemplates - template
            } else {
                state.selectedTemplates + template
            }
            val newDrafts = newSelected.map { t ->
                state.drafts.find { it.template == t } ?: OnboardingDraft(t)
            }
            state.copy(selectedTemplates = newSelected, drafts = newDrafts)
        }
    }

    fun updateDraftName(index: Int, name: String) {
        _uiState.update { state ->
            val newDrafts = state.drafts.toMutableList()
            if (index in newDrafts.indices) {
                newDrafts[index] = newDrafts[index].copy(displayName = name)
            }
            state.copy(drafts = newDrafts)
        }
    }

    fun updateDraftColor(index: Int, color: Long) {
        _uiState.update { state ->
            val newDrafts = state.drafts.toMutableList()
            if (index in newDrafts.indices) {
                newDrafts[index] = newDrafts[index].copy(color = color)
            }
            state.copy(drafts = newDrafts)
        }
    }

    fun setMorningMotivation(enabled: Boolean) =
        _uiState.update { it.copy(morningMotivation = enabled) }

    fun setHabitReminders(enabled: Boolean) =
        _uiState.update { it.copy(habitReminders = enabled) }

    fun completeOnboarding(onDone: () -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true) }
        try {
            val state = _uiState.value

            // Save notification preferences
            prefs.setDailyMotivationEnabled(state.morningMotivation)
            if (state.morningMotivation || state.habitReminders) {
                prefs.setNotificationsEnabled(true)
            }

            // Create selected habits in Room
            state.drafts.forEachIndexed { index, draft ->
                val t = draft.template
                val habit = Habit(
                    id = UUID.randomUUID().toString(),
                    name = draft.displayName,
                    description = "",
                    icon = draft.icon,
                    color = draft.color,
                    label = t.label,
                    trackingType = t.trackingType,
                    unit = t.unit,
                    targetValue = t.targetValue,
                    frequency = t.frequency,
                    scheduleDays = t.scheduleDays,
                    isArchived = false,
                    reminderEnabled = state.habitReminders && t.reminderTime != null,
                    reminderTime = if (state.habitReminders) t.reminderTime else null,
                    reminderDays = t.scheduleDays,
                    sortOrder = index,
                    createdAt = System.currentTimeMillis(),
                )
                habitRepository.insert(habit)
            }

            // Mark onboarding complete
            prefs.setOnboardingCompleted(true)
            onDone()
        } finally {
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
