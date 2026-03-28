package com.verdant.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.database.repository.EmotionalContextRepository
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.repository.TransactionRepository
import com.verdant.core.model.EmotionalContext
import com.verdant.core.model.InferredMood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val isLoading: Boolean = true,
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
) : ViewModel() {

    private val today = LocalDate.now()
    private val monthStart = today.withDayOfMonth(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val monthEnd = today.plusDays(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val now = System.currentTimeMillis()

    val uiState: StateFlow<SummaryDashboardUiState> = combine(
        habitRepository.observeActiveHabits(),
        habitEntryRepository.observeAllEntries(today, today),
        emotionalContextRepository.observeLatest(),
        transactionRepository.totalSpent(monthStart, monthEnd),
        transactionRepository.totalIncome(monthStart, monthEnd),
    ) { habits, entries, mood, spent, income ->
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

        SummaryDashboardUiState(
            totalHabits = habits.size,
            completedHabits = completedCount,
            completionPercent = percent,
            topHabits = topHabits,
            mood = mood,
            monthlySpent = spent ?: 0.0,
            monthlyIncome = income ?: 0.0,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryDashboardUiState(),
    )
}
