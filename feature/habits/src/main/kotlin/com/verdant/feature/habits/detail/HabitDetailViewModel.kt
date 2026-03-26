package com.verdant.feature.habits.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.ai.AIFeatureUnavailableException
import com.verdant.core.ai.HabitSummary
import com.verdant.core.ai.VerdantAI
import com.verdant.core.common.DayCellBuilder
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.database.usecase.LogEntryUseCase
import com.verdant.core.model.ChatBubble
import com.verdant.core.model.ChatMessage
import com.verdant.core.model.ChatState
import com.verdant.core.model.DayCell
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.TrackingType
import com.verdant.core.model.isScheduledForDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.verdant.core.common.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

enum class DetailTab(val label: String) {
    DETAILS("Details"),
    CALENDAR("Calendar"),
    AI_COACH("AI Coach"),
    HISTORY("History"),
}

data class HabitDetailUiState(
    val habit: Habit? = null,
    val entries: List<HabitEntry> = emptyList(),
    val allEntries: List<HabitEntry> = emptyList(),
    val cells: List<DayCell> = emptyList(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val completionRate: Float = 0f,
    val totalEntries: Int = 0,
    val averageValue: Double? = null,
    val selectedTab: DetailTab = DetailTab.DETAILS,
    val selectedMonth: LocalDate = LocalDate.now().withDayOfMonth(1),
    val retroEntry: HabitEntry? = null,
    val retroDate: LocalDate? = null,
    val chat: ChatState = ChatState(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val logEntryUseCase: LogEntryUseCase,
    private val verdantAI: VerdantAI,
) : ViewModel() {

    private val habitId: String = checkNotNull(savedStateHandle["habitId"])
    private val _selectedTab = MutableStateFlow(DetailTab.DETAILS)
    private val _selectedMonth = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val _retroDate = MutableStateFlow<LocalDate?>(null)
    private val _stats = MutableStateFlow(Triple(0, 0, 0f))
    private val _chatState = MutableStateFlow(ChatState())

    private val gridStart = LocalDate.now().minusWeeks(12)
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
        _chatState,
    ) { habit, entries, tab, month, retroDate, stats, chat ->
        val cells = DayCellBuilder.buildCells(entries, gridStart, gridEnd)
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
            allEntries = entries.sortedByDescending { it.date },
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
            chat = chat,
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

    fun onTabSelected(tab: DetailTab) = _selectedTab.update { tab }

    fun onMonthChanged(month: LocalDate) = _selectedMonth.update { month }

    fun onCellTapped(date: LocalDate) = _retroDate.update { date }

    fun onEntryTapped(entry: HabitEntry) = _retroDate.update { entry.date }

    fun onDismissRetroSheet() = _retroDate.update { null }

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

    fun archiveHabit() {
        viewModelScope.launch { habitRepository.archive(habitId) }
    }

    fun deleteHabit() {
        viewModelScope.launch {
            habitFlow.value?.let { habitRepository.delete(it) }
        }
    }

    // ── AI Coach Chat ──

    fun onChatInputChanged(text: String) {
        _chatState.update { it.copy(inputText = text, sendError = null) }
    }

    fun sendChatMessage() {
        val text = _chatState.value.inputText.trim()
        if (text.isBlank() || _chatState.value.isSending) return

        val userBubble = ChatBubble(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = text,
            timestamp = System.currentTimeMillis(),
        )
        val streamingId = UUID.randomUUID().toString()
        val streamingBubble = ChatBubble(
            id = streamingId,
            role = "assistant",
            content = "",
            timestamp = System.currentTimeMillis(),
            isStreaming = true,
        )

        _chatState.update { s ->
            s.copy(
                messages = s.messages + userBubble + streamingBubble,
                inputText = "",
                isSending = true,
                sendError = null,
            )
        }

        viewModelScope.launch {
            val result = runCatching { callHabitCoach(text) }
            result.fold(
                onSuccess = { reply ->
                    _chatState.update { s ->
                        s.copy(
                            messages = s.messages.map { bubble ->
                                if (bubble.id == streamingId) bubble.copy(content = reply, isStreaming = false)
                                else bubble
                            },
                            isSending = false,
                        )
                    }
                },
                onFailure = { e ->
                    val errorMsg = when (e) {
                        is AIFeatureUnavailableException -> when (e.reason) {
                            AIFeatureUnavailableException.Reason.NO_NETWORK ->
                                "Connect to the internet to use the AI coach"
                            AIFeatureUnavailableException.Reason.RATE_LIMITED ->
                                "Daily chat limit reached — try again tomorrow"
                            AIFeatureUnavailableException.Reason.AUTH_ERROR ->
                                "Please sign in to use the AI coach"
                            else -> "Something went wrong — please try again"
                        }
                        else -> "Something went wrong — please try again"
                    }
                    _chatState.update { s ->
                        s.copy(
                            messages = s.messages.map { bubble ->
                                if (bubble.id == streamingId) bubble.copy(
                                    content = errorMsg,
                                    isStreaming = false,
                                    isError = true,
                                ) else bubble
                            },
                            isSending = false,
                        )
                    }
                },
            )
        }
    }

    fun retryChatMessage() {
        val messages = _chatState.value.messages
        val lastUserBubble = messages.lastOrNull { it.role == "user" } ?: return
        _chatState.update { s ->
            s.copy(
                messages = s.messages.dropLastWhile { it.isError || it.isStreaming },
                inputText = lastUserBubble.content,
            )
        }
        _chatState.update { s ->
            s.copy(
                messages = s.messages.dropLastWhile { it.id == lastUserBubble.id },
            )
        }
        sendChatMessage()
    }

    fun clearChat() {
        _chatState.update { ChatState() }
    }

    fun onSuggestionClick(suggestion: String) {
        _chatState.update { it.copy(inputText = suggestion) }
        sendChatMessage()
    }

    private suspend fun callHabitCoach(userMessage: String): String {
        val habit = habitFlow.value ?: throw IllegalStateException("No habit loaded")
        val today = LocalDate.now()
        val start7d = today.minusDays(6)

        val entries7d = entryRepository.observeEntries(habitId, start7d, today).first()
        val streak = calculateStreakUseCase.currentStreak(habitId)
        val completedCount = entries7d.count { it.completed }
        val scheduledCount = (0 until 7).count { offset ->
            habit.isScheduledForDate(today.minusDays(offset.toLong()))
        }
        val weekRate = if (scheduledCount == 0) 0f else completedCount.toFloat() / scheduledCount

        val habitSummary = HabitSummary(
            habits = listOf(habit),
            recentCompletionRate = weekRate,
            activeStreaks = if (streak > 0) mapOf(habitId to streak) else emptyMap(),
            periodDays = 7,
        )

        val history = _chatState.value.messages
            .filter { !it.isError && !it.isStreaming }
            .map { bubble ->
                ChatMessage(
                    id = bubble.id,
                    role = bubble.role,
                    content = bubble.content,
                    habitContext = null,
                    timestamp = bubble.timestamp,
                )
            }

        return verdantAI.chatWithCoach(history, habitSummary)
    }
}
