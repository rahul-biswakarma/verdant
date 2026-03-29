package com.verdant.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.common.auth.AuthRepository
import com.verdant.core.common.auth.AuthUser
import com.verdant.core.model.repository.AIInsightRepository
import com.verdant.core.model.repository.EmotionalContextRepository
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.model.repository.StreakCacheRepository
import com.verdant.core.model.repository.TransactionRepository
import com.verdant.core.model.EmotionalContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class SummaryDashboardUiState(
    val totalHabits: Int = 0,
    val completedHabits: Int = 0,
    val completionPercent: Float = 0f,
    val topHabits: List<HabitSummary> = emptyList(),
    val mood: EmotionalContext? = null,
    val monthlySpent: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val bestCurrentStreak: Int = 0,
    val latestInsight: String? = null,
    val isLoading: Boolean = true,
)

data class ProfileSheetState(
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
)

data class HabitSummary(
    val id: String,
    val name: String,
    val icon: String,
    val color: Long,
    val completedToday: Boolean,
)

@HiltViewModel
class SummaryDashboardViewModel @Inject constructor(
    habitRepository: HabitRepository,
    habitEntryRepository: HabitEntryRepository,
    emotionalContextRepository: EmotionalContextRepository,
    transactionRepository: TransactionRepository,
    streakCacheRepository: StreakCacheRepository,
    aiInsightRepository: AIInsightRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val today = LocalDate.now()
    private val monthStart = today.withDayOfMonth(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val monthEnd = today.plusDays(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val currentUser: StateFlow<AuthUser?> = authRepository.currentUser

    private val _profileSheetState = MutableStateFlow(ProfileSheetState())
    val profileSheetState: StateFlow<ProfileSheetState> = _profileSheetState.asStateFlow()

    private data class CoreData(
        val totalHabits: Int,
        val completedHabits: Int,
        val completionPercent: Float,
        val topHabits: List<HabitSummary>,
        val mood: EmotionalContext?,
    )

    private data class FinanceAndExtra(
        val monthlySpent: Double,
        val monthlyIncome: Double,
        val bestCurrentStreak: Int,
        val latestInsight: String?,
    )

    private val coreData = combine(
        habitRepository.observeActiveHabits(),
        habitEntryRepository.observeAllEntries(today, today),
        emotionalContextRepository.observeLatest(),
    ) { habits, entries, mood ->
        val completedIds = entries
            .filter { it.completed }
            .map { it.habitId }
            .toSet()

        val completedCount = habits.count { it.id in completedIds }
        val percent = if (habits.isNotEmpty()) completedCount.toFloat() / habits.size else 0f

        val topHabits = habits
            .take(3)
            .map { habit ->
                HabitSummary(
                    id = habit.id,
                    name = habit.name,
                    icon = habit.icon,
                    color = habit.color,
                    completedToday = habit.id in completedIds,
                )
            }

        CoreData(
            totalHabits = habits.size,
            completedHabits = completedCount,
            completionPercent = percent,
            topHabits = topHabits,
            mood = mood,
        )
    }

    private val financeAndExtra = combine(
        transactionRepository.totalSpent(monthStart, monthEnd),
        transactionRepository.totalIncome(monthStart, monthEnd),
        streakCacheRepository.observeAll(),
        aiInsightRepository.observeRecent(1),
    ) { spent, income, streaks, insights ->
        FinanceAndExtra(
            monthlySpent = spent ?: 0.0,
            monthlyIncome = income ?: 0.0,
            bestCurrentStreak = streaks.maxOfOrNull { it.currentStreak } ?: 0,
            latestInsight = insights.firstOrNull()?.content,
        )
    }

    val uiState: StateFlow<SummaryDashboardUiState> = combine(
        coreData,
        financeAndExtra,
    ) { core, extra ->
        SummaryDashboardUiState(
            totalHabits = core.totalHabits,
            completedHabits = core.completedHabits,
            completionPercent = core.completionPercent,
            topHabits = core.topHabits,
            mood = core.mood,
            monthlySpent = extra.monthlySpent,
            monthlyIncome = extra.monthlyIncome,
            bestCurrentStreak = extra.bestCurrentStreak,
            latestInsight = extra.latestInsight,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryDashboardUiState(),
    )

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            _profileSheetState.update { it.copy(isUpdating = true, errorMessage = null) }
            val result = authRepository.updateProfile(displayName = newName)
            _profileSheetState.update {
                if (result.isSuccess) ProfileSheetState()
                else it.copy(isUpdating = false, errorMessage = "Failed to update name")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearProfileError() {
        _profileSheetState.update { it.copy(errorMessage = null) }
    }
}
