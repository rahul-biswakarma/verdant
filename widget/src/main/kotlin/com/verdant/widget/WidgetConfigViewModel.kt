package com.verdant.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.model.Habit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class WidgetConfigUiState(
    val habits: List<Habit> = emptyList(),
    val selectedHabitId: String? = null,
    val colorTheme: String = "habit",   // "habit" | "dark" | "light"
    val gridDensity: String = "comfortable", // "comfortable" | "compact"
    val isLoading: Boolean = true,
)

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    habitRepository: HabitRepository,
) : ViewModel() {

    private val _selectedHabitId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val _colorTheme = kotlinx.coroutines.flow.MutableStateFlow("habit")
    private val _gridDensity = kotlinx.coroutines.flow.MutableStateFlow("comfortable")

    val habits: StateFlow<List<Habit>> = habitRepository
        .observeActiveHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedHabitId: StateFlow<String?> = _selectedHabitId
    val colorTheme: StateFlow<String> = _colorTheme
    val gridDensity: StateFlow<String> = _gridDensity

    fun selectHabit(habitId: String) = _selectedHabitId.tryEmit(habitId)
    fun setColorTheme(theme: String) = _colorTheme.tryEmit(theme)
    fun setGridDensity(density: String) = _gridDensity.tryEmit(density)
}
