package com.verdant.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.ai.BrainDumpAction
import com.verdant.core.ai.MotivationContext
import com.verdant.core.ai.ParsedBrainDump
import com.verdant.core.ai.VerdantAI
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.common.usecase.CalculateStreakUseCase
import com.verdant.core.common.usecase.LogEntryUseCase
import com.verdant.core.common.usecase.StreakCacheManager
import androidx.compose.ui.graphics.Color
import com.verdant.core.designsystem.component.CelebrationData
import com.verdant.core.designsystem.component.OrbitalHabitData
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import com.verdant.core.model.isScheduledForDate
import com.verdant.core.voice.VoiceCommandParser
import com.verdant.core.voice.VoiceRecognitionManager
import java.time.temporal.ChronoUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val eventDrivenItems: List<OrbitalHabitData> = emptyList(),
    val timerRunning: Set<String> = emptySet(),
    val timerSeconds: Map<String, Int> = emptyMap(),
    val aiInsight: String? = null,
    val isLoading: Boolean = true,
    val hasAnyHabits: Boolean = true,
)

data class BrainDumpUiState(
    val text: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val result: ParsedBrainDump? = null,
)

// Displayed in the "Log expense" dialog
data class FinancialDialogState(
    val habitId: String,
    val amountInput: String = "",
    val categoryInput: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val logEntryUseCase: LogEntryUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val streakCacheManager: StreakCacheManager,
    private val verdantAI: VerdantAI,
    val voiceRecognition: VoiceRecognitionManager,
    private val voiceCommandParser: VoiceCommandParser,
) : ViewModel() {

    private val today = LocalDate.now()

    private val _timerRunning = MutableStateFlow<Set<String>>(emptySet())
    private val _timerSeconds = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val timerJobs = mutableMapOf<String, Job>()

    private val _streaks = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _aiInsight = MutableStateFlow<String?>(null)

    private val _brainDump = MutableStateFlow(BrainDumpUiState())
    val brainDumpState: StateFlow<BrainDumpUiState> = _brainDump

    private val _celebration = MutableStateFlow<CelebrationData?>(null)
    val celebration: StateFlow<CelebrationData?> = _celebration

    val uiState: StateFlow<HomeUiState> = com.verdant.core.common.combine(
        habitRepository.observeActiveHabits(),
        entryRepository.observeAllEntries(today, today),
        _streaks,
        _timerRunning,
        _timerSeconds,
        _aiInsight,
    ) { habits, entries, streaks, timerRunning, timerSeconds, aiInsight ->
        val todayHabits = habits.filter {
            it.trackingType != TrackingType.EVENT_DRIVEN && it.isScheduledForDate(today)
        }
        val entryMap = entries.associateBy { it.habitId }
        val completed = todayHabits.count { habit ->
            val entry = entryMap[habit.id]
            when (habit.trackingType) {
                TrackingType.BINARY -> entry?.completed == true
                else -> entry?.completed == true
            }
        }
        // Build orbital data for EVENT_DRIVEN habits
        val eventDrivenHabits = habits.filter { it.trackingType == TrackingType.EVENT_DRIVEN }
        val eventDrivenItems = eventDrivenHabits.map { habit ->
            val lastDate = entryRepository.getCompletedDates(habit.id).maxOrNull()
            val daysSinceLast = if (lastDate != null) {
                ChronoUnit.DAYS.between(lastDate, today).toInt()
            } else -1
            OrbitalHabitData(
                habitId = habit.id,
                habitName = habit.name,
                habitIcon = habit.icon,
                color = Color(habit.color),
                daysSinceLast = daysSinceLast,
                maxDaysBeforeUrgent = habit.targetValue?.toInt()?.takeIf { it > 0 } ?: 7,
            )
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
            eventDrivenItems = eventDrivenItems,
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
                val newStreaks = streakCacheManager.getCurrentStreaks(todayHabits.map { it.id })
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
            streakCacheManager.invalidate(item.habit.id)
            if (newCompleted) checkMilestone(item.habit)
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

    // ── Event-driven ─────────────────────────────────────────────────────────

    /** Logs a completion for an EVENT_DRIVEN habit, snapping its orbit back to center. */
    fun logEventDriven(habitId: String) {
        viewModelScope.launch {
            logEntryUseCase.logBinary(habitId, today, completed = true)
        }
    }

    // ── Location ─────────────────────────────────────────────────────────────

    fun checkIn(item: TodayHabitItem) {
        viewModelScope.launch {
            logEntryUseCase.logLocation(item.habit.id, today, null, null)
        }
    }

    // ── Brain Dump ────────────────────────────────────────────────────────────

    fun onBrainDumpTextChange(text: String) {
        _brainDump.update { it.copy(text = text, error = null) }
    }

    fun onBrainDumpSubmit() {
        val text = _brainDump.value.text.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _brainDump.update { it.copy(isLoading = true, error = null, result = null) }
            val habits = habitRepository.getAllHabits()
                .filter { !it.isArchived && it.isScheduledForDate(today) }
            runCatching { verdantAI.parseBrainDump(text, habits) }
                .onSuccess { parsed ->
                    _brainDump.update { it.copy(isLoading = false, result = parsed) }
                }
                .onFailure { e ->
                    _brainDump.update {
                        it.copy(isLoading = false, error = e.message ?: "Could not parse your entry")
                    }
                }
        }
    }

    fun onBrainDumpConfirm() {
        val result = _brainDump.value.result ?: return
        viewModelScope.launch {
            val itemMap = uiState.value.todayItems.associateBy { it.habit.name }
            result.entries.forEach { entry ->
                val item = itemMap[entry.habitName] ?: return@forEach
                when (entry.action) {
                    BrainDumpAction.SKIPPED -> logEntryUseCase.skip(item.habit.id, today)
                    BrainDumpAction.LOGGED -> {
                        val entryValue = entry.value
                        when {
                            entryValue != null -> logEntryUseCase.addQuantitative(
                                item.habit.id, today, entryValue, item.habit.targetValue,
                            )
                            else -> logEntryUseCase.logBinary(item.habit.id, today, true)
                        }
                    }
                }
            }
            _brainDump.value = BrainDumpUiState() // reset
        }
    }

    fun onBrainDumpDismiss() {
        _brainDump.update { it.copy(result = null, error = null) }
    }

    fun dismissCelebration() {
        _celebration.value = null
    }

    // ── Voice Input ──────────────────────────────────────────────────────────

    fun startVoiceInput() {
        voiceRecognition.startListening()
    }

    fun stopVoiceInput() {
        voiceRecognition.stopListening()
    }

    fun processVoiceResult(text: String) {
        val commands = voiceCommandParser.parse(text)
        val items = uiState.value.todayItems
        viewModelScope.launch {
            for (cmd in commands) {
                // Fuzzy match habit name
                val item = items.minByOrNull {
                    levenshtein(it.habit.name.lowercase(), cmd.habitName.lowercase())
                }?.takeIf {
                    levenshtein(it.habit.name.lowercase(), cmd.habitName.lowercase()) <= it.habit.name.length / 2
                } ?: continue

                val cmdValue = cmd.value
                when {
                    cmdValue != null -> logEntryUseCase.addQuantitative(
                        item.habit.id, today, cmdValue, item.habit.targetValue,
                    )
                    else -> logEntryUseCase.logBinary(item.habit.id, today, true)
                }
            }
            voiceRecognition.resetState()
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1,
                )
            }
        }
        return dp[a.length][b.length]
    }

    private suspend fun checkMilestone(habit: Habit) {
        val streak = streakCacheManager.getCurrentStreak(habit.id)
        val milestone = MILESTONES.firstOrNull { streak == it }
        if (milestone != null) {
            val message = runCatching { verdantAI.generateMilestoneMessage(habit, milestone) }
                .getOrDefault("You've kept up ${habit.name} for $milestone days!")
            _celebration.value = CelebrationData(habit.name, milestone, message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJobs.values.forEach { it.cancel() }
    }
}

private val MILESTONES = listOf(365, 100, 30, 14, 7, 3)

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
