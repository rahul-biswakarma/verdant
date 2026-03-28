package com.verdant.feature.settings.buddies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.model.Habit
import com.verdant.core.social.BuddyConnection
import com.verdant.core.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuddyUiState(
    val habits: List<Habit> = emptyList(),
    val buddiesByHabit: Map<String, List<BuddyConnection>> = emptyMap(),
    val isLoading: Boolean = true,
    val inviteCode: String? = null,
    val inviteDialogHabitId: String? = null,
    val joinDialogOpen: Boolean = false,
    val joinCode: String = "",
    val joinError: String? = null,
    val joinSuccess: Boolean = false,
)

@HiltViewModel
class BuddyViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val habitRepository: HabitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuddyUiState())
    val uiState: StateFlow<BuddyUiState> = _uiState

    init {
        loadHabitsAndBuddies()
    }

    private fun loadHabitsAndBuddies() {
        viewModelScope.launch {
            val habits = habitRepository.getAllHabits().filter { !it.isArchived }
            _uiState.update { it.copy(habits = habits, isLoading = false) }

            // Observe buddies for each habit
            for (habit in habits) {
                viewModelScope.launch {
                    socialRepository.observeBuddies(habit.id).collect { buddies ->
                        _uiState.update { state ->
                            val updated = state.buddiesByHabit.toMutableMap()
                            if (buddies.isNotEmpty()) {
                                updated[habit.id] = buddies
                            } else {
                                updated.remove(habit.id)
                            }
                            state.copy(buddiesByHabit = updated)
                        }
                    }
                }
            }
        }
    }

    fun createInvite(habitId: String, habitName: String) {
        viewModelScope.launch {
            val code = socialRepository.createInvite(habitId, habitName)
            _uiState.update { it.copy(inviteCode = code, inviteDialogHabitId = habitId) }
        }
    }

    fun dismissInviteDialog() {
        _uiState.update { it.copy(inviteCode = null, inviteDialogHabitId = null) }
    }

    fun openJoinDialog() {
        _uiState.update { it.copy(joinDialogOpen = true, joinCode = "", joinError = null, joinSuccess = false) }
    }

    fun dismissJoinDialog() {
        _uiState.update { it.copy(joinDialogOpen = false, joinCode = "", joinError = null, joinSuccess = false) }
    }

    fun onJoinCodeChange(code: String) {
        _uiState.update { it.copy(joinCode = code, joinError = null) }
    }

    fun acceptInvite() {
        val code = _uiState.value.joinCode.trim()
        if (code.length != 6) {
            _uiState.update { it.copy(joinError = "Enter a 6-digit invite code") }
            return
        }
        viewModelScope.launch {
            val success = socialRepository.acceptInvite(code)
            if (success) {
                _uiState.update { it.copy(joinSuccess = true, joinError = null) }
                loadHabitsAndBuddies() // Refresh
            } else {
                _uiState.update { it.copy(joinError = "Invalid or expired invite code") }
            }
        }
    }
}
