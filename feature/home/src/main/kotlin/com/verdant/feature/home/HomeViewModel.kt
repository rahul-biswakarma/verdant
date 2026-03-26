package com.verdant.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.ai.BrainDumpAction
import com.verdant.core.ai.BrainDumpResult
import com.verdant.core.ai.MotivationContext
import com.verdant.core.ai.VerdantAI
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.database.usecase.LogEntryUseCase
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import com.verdant.core.model.isScheduledForDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TodayHabitItem(
    val habit: Habit,
    val entry: HabitEntry?,
    val streak: Int,
)

data class HomeUiState(
    val greeting: String = "",
    val formattedDate: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val todayItems: List<TodayHabitItem> = emptyList(),
    val timerRunning: Set<String> = emptySet(),
    val timerSeconds: Map<String, Int> = emptyMap(),
    val aiInsight: String? = null,
    val isLoading: Boolean = true,
    val hasAnyHabits: Boolean = true,
)

// Displayed in the "Log expense" dialog
data class FinancialDialogState(
    val habitId: String,
    val amountInput: String = "",
    val categoryInput: String = "",
)

data class BrainDumpState(
    val input: String = "",
    val isLoading: Boolean = false,
    /** Non-null once the AI has parsed a submission; null means the sheet is hidden. */
    val results: List<BrainDumpResult>? = null,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val logEntryUseCase: LogEntryUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val verdantAI: VerdantAI,
) : ViewModel() {

    private val today = LocalDate.now()

    private val _timerRunning = MutableStateFlow<Set<String>>(emptySet())
    private val _timerSeconds = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val timerJobs = mutableMapOf<String, Job>()

    private val _streaks = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _aiInsight = MutableStateFlow<String?>(null)

    private val _brainDump = MutableStateFlow(BrainDumpState())
    val brainDumpState: StateFlow<BrainDumpState> = _brainDump.asStateFlow()

    val uiState: StateFlow<HomeUiState> = com.verdant.core.common.combine(
        habitRepository.observeActiveHabits(),
        entryRepository.observeAllEntries(today, today),
        _streaks,
        _timerRunning,
        _timerSeconds,
        _aiInsight,
    ) { habits, entries, streaks, timerRunning, timerSeconds, aiInsight ->
        val todayHabits = habits.filter { it.isScheduledForDate(today) }
        val entryMap = entries.associateBy { it.habitId }
        val completed = todayHabits.count { habit ->
            val entry = entryMap[habit.id]
            when (habit.trackingType) {
                TrackingType.BINARY -> entry?.completed == true
                else -> entry?.completed == true
            }
        }
        HomeUiState(
            greeting = greeting(),
            formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            completedCount = completed,
            totalCount = todayHabits.size,
            todayItems = todayHabits.map { habit ->
                TodayHabitItem(
                    habit = habit,
                    entry = entryMap[habit.id],
                    streak = streaks[habit.id] ?: 0,
                )
            },
            timerRunning = timerRunning,
            timerSeconds = timerSeconds,
            aiInsight = aiInsight,
            isLoading = false,
            hasAnyHabits = habits.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        loadStreaks()
        loadAiInsight()
    }

    private fun loadStreaks() {
        viewModelScope.launch {
            val habits = habitRepository.observeActiveHabits()
            habits.collect { list ->
                val todayHabits = list.filter { it.isScheduledForDate(today) }
                val newStreaks = calculateStreakUseCase.currentStreaks(todayHabits.map { it.id })
                _streaks.value = newStreaks
            }
        }
    }

    private fun loadAiInsight() {
        viewModelScope.launch {
            try {
                val habits = habitRepository.getAllHabits()
                val todayHabits = habits.filter { !it.isArchived && it.isScheduledForDate(today) }
                if (todayHabits.isEmpty()) return@launch

                val streaks = _streaks.value
                val ctx = MotivationContext(
                    todayHabits = todayHabits,
                    activeStreaks = streaks,
                    yesterdayCompletion = 0f,
                    weekCompletion = 0f,
                )
                val insight = verdantAI.generateMotivation(ctx)
                if (insight.isNotBlank()) {
                    _aiInsight.value = insight
                }
            } catch (_: Exception) {
                // Silently fail — insight card will show default text
            }
        }
    }

    // ── Binary ───────────────────────────────────────────────────────────────

    fun toggleBinary(item: TodayHabitItem) {
        val newCompleted = item.entry?.completed?.not() ?: true
        viewModelScope.launch {
            logEntryUseCase.logBinary(item.habit.id, today, newCompleted)
        }
    }

    // ── Quantitative ─────────────────────────────────────────────────────────

    fun addQuantitative(item: TodayHabitItem, delta: Double) {
        viewModelScope.launch {
            logEntryUseCase.addQuantitative(item.habit.id, today, delta, item.habit.targetValue)
        }
    }

    fun setQuantitative(item: TodayHabitItem, value: Double) {
        viewModelScope.launch {
            logEntryUseCase.setQuantitative(item.habit.id, today, value, item.habit.targetValue)
        }
    }

    // ── Duration / Timer ─────────────────────────────────────────────────────

    fun startTimer(habitId: String) {
        timerJobs[habitId]?.cancel()
        _timerRunning.update { it + habitId }
        timerJobs[habitId] = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _timerSeconds.update { map ->
                    map + (habitId to (map[habitId] ?: 0) + 1)
                }
            }
        }
    }

    fun stopAndLogTimer(item: TodayHabitItem) {
        timerJobs[item.habit.id]?.cancel()
        timerJobs.remove(item.habit.id)
        _timerRunning.update { it - item.habit.id }

        val seconds = _timerSeconds.value[item.habit.id] ?: 0
        _timerSeconds.update { it - item.habit.id }

        if (seconds > 0) {
            val minutes = seconds / 60.0
            viewModelScope.launch {
                logEntryUseCase.addQuantitative(item.habit.id, today, minutes, item.habit.targetValue)
            }
        }
    }

    fun setDurationManually(item: TodayHabitItem, minutes: Double) {
        viewModelScope.launch {
            logEntryUseCase.setQuantitative(item.habit.id, today, minutes, item.habit.targetValue)
        }
    }

    // ── Financial ────────────────────────────────────────────────────────────

    fun logFinancial(item: TodayHabitItem, amount: Double, category: String?) {
        viewModelScope.launch {
            logEntryUseCase.logFinancial(item.habit.id, today, amount, category, item.habit.targetValue)
        }
    }

    // ── Location ─────────────────────────────────────────────────────────────

    fun checkIn(item: TodayHabitItem) {
        viewModelScope.launch {
            logEntryUseCase.logLocation(item.habit.id, today, null, null)
        }
    }

    // ── Brain Dump ───────────────────────────────────────────────────────────

    fun onBrainDumpInputChange(text: String) {
        _brainDump.update { it.copy(input = text, error = null) }
    }

    fun submitBrainDump() {
        val text = _brainDump.value.input.trim()
        if (text.isBlank()) return
        _brainDump.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val habits = habitRepository.getAllHabits()
                    .filter { !it.isArchived && it.isScheduledForDate(today) }
                if (habits.isEmpty()) {
                    _brainDump.update { it.copy(isLoading = false, error = "No habits scheduled for today") }
                    return@launch
                }
                val results = verdantAI.parseBrainDump(text, habits)
                if (results.isEmpty()) {
                    _brainDump.update { it.copy(isLoading = false, error = "Couldn't match any habits — try mentioning them by name") }
                } else {
                    _brainDump.update { it.copy(isLoading = false, results = results) }
                }
            } catch (_: Exception) {
                _brainDump.update { it.copy(isLoading = false, error = "Couldn't parse your log — try again") }
            }
        }
    }

    fun confirmBrainDump(results: List<BrainDumpResult>) {
        viewModelScope.launch {
            results.forEach { result ->
                when (result.action) {
                    BrainDumpAction.COMPLETE -> when (result.habit.trackingType) {
                        TrackingType.LOCATION -> logEntryUseCase.logLocation(result.habit.id, today, null, null)
                        else -> logEntryUseCase.logBinary(result.habit.id, today, true)
                    }
                    BrainDumpAction.SKIP -> logEntryUseCase.skip(result.habit.id, today)
                    BrainDumpAction.SET_VALUE -> {
                        val value = result.value ?: return@forEach
                        when (result.habit.trackingType) {
                            TrackingType.FINANCIAL -> logEntryUseCase.logFinancial(
                                result.habit.id, today, value, null, result.habit.targetValue,
                            )
                            else -> logEntryUseCase.setQuantitative(
                                result.habit.id, today, value, result.habit.targetValue,
                            )
                        }
                    }
                }
            }
            _brainDump.update { BrainDumpState() }
        }
    }

    fun dismissBrainDump() {
        _brainDump.update { it.copy(results = null) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJobs.values.forEach { it.cancel() }
    }
}

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
