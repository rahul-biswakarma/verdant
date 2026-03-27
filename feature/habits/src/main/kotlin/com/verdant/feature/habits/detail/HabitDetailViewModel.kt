package com.verdant.feature.habits.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.database.usecase.LogEntryUseCase
import com.verdant.core.model.DayCell
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.verdant.core.common.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class HabitDetailUiState(
    val habit: Habit? = null,
    val entries: List<HabitEntry> = emptyList(),
    val cells: List<DayCell> = emptyList(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val completionRate: Float = 0f,
    val totalEntries: Int = 0,
    val averageValue: Double? = null,
    val selectedTab: Int = 0,          // 0 = history, 1 = calendar, 2 = mood (EMOTIONAL only)
    val selectedMonth: LocalDate = LocalDate.now().withDayOfMonth(1),
    val retroEntry: HabitEntry? = null, // entry being edited in bottom sheet
    val retroDate: LocalDate? = null,   // date for which bottom sheet is open
    val moodYear: Int = LocalDate.now().year,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val logEntryUseCase: LogEntryUseCase,
) : ViewModel() {

    private val habitId: String = checkNotNull(savedStateHandle["habitId"])
    private val _selectedTab = MutableStateFlow(0)
    private val _selectedMonth = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val _retroDate = MutableStateFlow<LocalDate?>(null)
    private val _stats = MutableStateFlow(Triple(0, 0, 0f)) // current, longest, rate
    private val _moodYear = MutableStateFlow(LocalDate.now().year)

    // Cover a full year for the mood grid; 12-week contribution grid uses the same entries
    private val gridStart = LocalDate.now().withDayOfYear(1)
    private val gridEnd = LocalDate.now()

    private val habitFlow = MutableStateFlow<Habit?>(null)
    private val entriesFlow = entryRepository.observeEntries(habitId, gridStart, gridEnd)

    val uiState: StateFlow<HabitDetailUiState> = combine(
        habitFlow.filterNotNull(),
        entriesFlow,
        _selectedTab,
        _selectedMonth,
        _retroDate,
        _stats,
    ) { habit, entries, tab, month, retroDate, stats ->
        val cells = buildCells(entries, gridStart, gridEnd)
        val avgValue = entries
            .filter { it.value != null }
            .map { it.value!! }
            .takeIf { it.isNotEmpty() }
            ?.average()
        val retroEntry = retroDate?.let {
            entries.firstOrNull { e -> e.date == it }
        }
        HabitDetailUiState(
            habit = habit,
            entries = entries.sortedByDescending { it.date }.take(14),
            cells = cells,
            currentStreak = stats.first,
            longestStreak = stats.second,
            completionRate = stats.third,
            totalEntries = entries.count { it.completed },
            averageValue = avgValue,
            selectedTab = tab,
            selectedMonth = month,
            retroEntry = retroEntry,
            retroDate = retroDate,
            moodYear = _moodYear.value,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState())

    init {
        viewModelScope.launch {
            habitRepository.getById(habitId)?.let { habitFlow.value = it }
            val current = calculateStreakUseCase.currentStreak(habitId)
            val longest = calculateStreakUseCase.longestStreak(habitId)
            val rate = calculateStreakUseCase.completionRate(habitId)
            _stats.value = Triple(current, longest, rate)
        }
    }

    fun onTabSelected(index: Int) = _selectedTab.update { index }

    fun onMonthChanged(month: LocalDate) = _selectedMonth.update { month }

    fun onCellTapped(date: LocalDate) = _retroDate.update { date }

    fun onEntryTapped(entry: HabitEntry) = _retroDate.update { entry.date }

    fun onDismissRetroSheet() = _retroDate.update { null }

    // ── Retro logging ────────────────────────────────────────────────────────

    fun retroLogBinary(date: LocalDate, completed: Boolean) {
        viewModelScope.launch { logEntryUseCase.logBinary(habitId, date, completed) }
    }

    fun retroSetQuantitative(date: LocalDate, value: Double) {
        viewModelScope.launch {
            val target = habitFlow.value?.targetValue
            logEntryUseCase.setQuantitative(habitId, date, value, target)
        }
    }

    fun retroSkip(date: LocalDate) {
        viewModelScope.launch { logEntryUseCase.skip(habitId, date) }
    }

    fun retroSaveEntry(entry: HabitEntry) {
        viewModelScope.launch { logEntryUseCase.upsertEntry(entry) }
    }

    fun retroDeleteEntry(entry: HabitEntry) {
        viewModelScope.launch { entryRepository.delete(entry) }
    }

    fun retroLogMood(date: LocalDate, score: Int, note: String?) {
        viewModelScope.launch { logEntryUseCase.logMood(habitId, date, score, note) }
    }

    fun onMoodYearChanged(year: Int) = _moodYear.update { year }

    // ── Habit actions ────────────────────────────────────────────────────────

    fun archiveHabit() {
        viewModelScope.launch { habitRepository.archive(habitId) }
    }

    fun deleteHabit() {
        viewModelScope.launch {
            habitFlow.value?.let { habitRepository.delete(it) }
        }
    }

    private fun buildCells(
        entries: List<HabitEntry>,
        start: LocalDate,
        end: LocalDate,
    ): List<DayCell> {
        val entryMap = entries.associateBy { it.date }
        val days = start.datesUntil(end.plusDays(1)).toList()
        return days.map { date ->
            val entry = entryMap[date]
            DayCell(
                date = date,
                intensity = when {
                    entry == null -> 0f
                    entry.skipped -> 0f
                    entry.completed -> 1f
                    entry.value != null -> 0.5f
                    else -> 0f
                },
                entryCount = if (entry != null) 1 else 0,
                completedCount = if (entry?.completed == true) 1 else 0,
            )
        }
    }
}
