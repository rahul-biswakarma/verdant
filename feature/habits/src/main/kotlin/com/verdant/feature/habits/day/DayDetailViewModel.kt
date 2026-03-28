package com.verdant.feature.habits.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.common.usecase.LogEntryUseCase
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.isScheduledForDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DayDetailUiState(
    val date: LocalDate? = null,
    /** Pairs of (Habit, today's entry or null) */
    val items: List<Pair<Habit, HabitEntry?>> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val logEntryUseCase: LogEntryUseCase,
) : ViewModel() {

    /** Date is passed as ISO yyyy-MM-dd string via navigation arg */
    private val date: LocalDate = savedStateHandle.get<String>("date")
        ?.let { LocalDate.parse(it) }
        ?: LocalDate.now()

    val uiState: StateFlow<DayDetailUiState> = combine(
        habitRepository.observeActiveHabits(),
        entryRepository.observeAllEntries(date, date),
    ) { habits, entries ->
        val entryMap = entries.associateBy { it.habitId }
        val scheduled = habits.filter { it.isScheduledForDate(date) }
        DayDetailUiState(
            date = date,
            items = scheduled.map { it to entryMap[it.id] },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayDetailUiState(date = date))

    fun logBinary(habit: Habit, completed: Boolean) {
        viewModelScope.launch { logEntryUseCase.logBinary(habit.id, date, completed) }
    }

    fun logQuantitative(habit: Habit, value: Double) {
        viewModelScope.launch { logEntryUseCase.setQuantitative(habit.id, date, value, habit.targetValue) }
    }

    fun skip(habit: Habit) {
        viewModelScope.launch { logEntryUseCase.skip(habit.id, date) }
    }
}
