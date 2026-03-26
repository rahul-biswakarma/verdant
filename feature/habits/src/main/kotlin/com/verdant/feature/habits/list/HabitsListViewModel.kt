package com.verdant.feature.habits.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.common.DayCellBuilder
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.repository.LabelRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.model.DayCell
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.Label
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitsListUiState(
    val habits: List<Habit> = emptyList(),
    val todayEntries: Map<String, HabitEntry> = emptyMap(),
    val streaks: Map<String, Int> = emptyMap(),
    val recentCells: Map<String, List<DayCell>> = emptyMap(),
    val labels: List<Label> = emptyList(),
    val selectedLabel: String? = null,
    val isLoading: Boolean = true,
) {
    val filtered: List<Habit> get() =
        if (selectedLabel == null) habits
        else habits.filter { it.label == selectedLabel }
}

@HiltViewModel
class HabitsListViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val labelRepository: LabelRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
) : ViewModel() {

    private val today = LocalDate.now()
    private val gridStart = today.minusWeeks(4)
    private val _selectedLabel = MutableStateFlow<String?>(null)
    private val _streaks = MutableStateFlow<Map<String, Int>>(emptyMap())

    val uiState: StateFlow<HabitsListUiState> = combine(
        habitRepository.observeActiveHabits(),
        entryRepository.observeAllEntries(gridStart, today),
        labelRepository.observeAll(),
        _selectedLabel,
        _streaks,
    ) { habits, entries, labels, selected, streaks ->
        val todayEntries = entries.filter { it.date == today }.associateBy { it.habitId }
        val entriesByHabit = entries.groupBy { it.habitId }
        val recentCells = habits.associate { habit ->
            habit.id to DayCellBuilder.buildCells(
                entries = entriesByHabit[habit.id] ?: emptyList(),
                start = gridStart,
                end = today,
            )
        }
        HabitsListUiState(
            habits = habits,
            todayEntries = todayEntries,
            recentCells = recentCells,
            labels = labels,
            selectedLabel = selected,
            streaks = streaks,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsListUiState())

    init {
        viewModelScope.launch {
            habitRepository.observeActiveHabits().collect { habits ->
                _streaks.value = calculateStreakUseCase.currentStreaks(habits.map { it.id })
            }
        }
    }

    fun onLabelSelected(label: String?) {
        _selectedLabel.update { if (it == label) null else label }
    }

    fun onArchiveHabit(habit: Habit) {
        viewModelScope.launch { habitRepository.archive(habit.id) }
    }

    fun onDeleteHabit(habit: Habit) {
        viewModelScope.launch { habitRepository.delete(habit) }
    }
}
