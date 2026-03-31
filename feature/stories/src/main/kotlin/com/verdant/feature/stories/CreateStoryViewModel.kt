package com.verdant.feature.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.model.Story
import com.verdant.core.model.StoryEvent
import com.verdant.core.model.StoryEventType
import com.verdant.core.model.StoryTemplate
import com.verdant.core.model.TransactionType
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.model.repository.StoryEventRepository
import com.verdant.core.model.repository.StoryRepository
import com.verdant.core.model.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateStoryViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val storyEventRepository: StoryEventRepository,
    private val habitRepository: HabitRepository,
    private val habitEntryRepository: HabitEntryRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateStoryUiState())
    val uiState: StateFlow<CreateStoryUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun selectTemplate(template: StoryTemplate?) {
        _uiState.update { it.copy(selectedTemplate = template) }
    }

    fun updateStartTime(time: Long) {
        _uiState.update { it.copy(startTime = time) }
        suggestEvents()
    }

    fun updateEndTime(time: Long?) {
        _uiState.update { it.copy(endTime = time) }
        suggestEvents()
    }

    fun updateCoverEmoji(emoji: String) {
        _uiState.update { it.copy(coverEmoji = emoji) }
    }

    fun addManualEvent(title: String, description: String?, type: StoryEventType) {
        val event = DraftEvent(
            eventType = type,
            title = title,
            description = description,
            timestamp = System.currentTimeMillis(),
        )
        _uiState.update { it.copy(events = it.events + event) }
    }

    fun removeEvent(index: Int) {
        _uiState.update { state ->
            state.copy(events = state.events.filterIndexed { i, _ -> i != index })
        }
    }

    fun toggleSuggestion(index: Int) {
        _uiState.update { state ->
            val updated = state.suggestedEvents.toMutableList()
            val item = updated[index]
            updated[index] = item.copy(isSelected = !item.isSelected)
            state.copy(suggestedEvents = updated)
        }
    }

    fun suggestEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuggestions = true) }

            val state = _uiState.value
            val start = state.startTime
            val end = state.endTime ?: System.currentTimeMillis()
            val suggestions = mutableListOf<SuggestedEvent>()

            // Suggest habit completions in the time range
            val zone = ZoneId.systemDefault()
            val startDate = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
            val endDate = Instant.ofEpochMilli(end).atZone(zone).toLocalDate()
            val habits = habitRepository.getAllHabits()

            var date = startDate
            while (!date.isAfter(endDate)) {
                for (habit in habits) {
                    val entry = habitEntryRepository.getByHabitAndDate(habit.id, date)
                    if (entry?.completed == true) {
                        suggestions.add(
                            SuggestedEvent(
                                eventType = StoryEventType.HABIT_COMPLETION,
                                title = "${habit.icon} ${habit.name}",
                                referenceId = entry.id,
                                timestamp = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                            ),
                        )
                    }
                }
                date = date.plusDays(1)
            }

            // Suggest transactions in the time range
            val transactions = transactionRepository.observeByDateRange(start, end).first()
            for (txn in transactions.take(10)) {
                val prefix = if (txn.type == TransactionType.DEBIT) "Spent" else "Received"
                suggestions.add(
                    SuggestedEvent(
                        eventType = StoryEventType.TRANSACTION,
                        title = "$prefix ₹${txn.amount.toInt()} ${txn.merchant?.let { "at $it" } ?: ""}",
                        referenceId = txn.id,
                        timestamp = txn.transactionDate,
                    ),
                )
            }

            suggestions.sortBy { it.timestamp }

            _uiState.update {
                it.copy(
                    suggestedEvents = suggestions,
                    isLoadingSuggestions = false,
                )
            }
        }
    }

    fun saveStory(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.title.isBlank()) return@launch

            _uiState.update { it.copy(isSaving = true) }

            val now = System.currentTimeMillis()
            val storyId = UUID.randomUUID().toString()

            val story = Story(
                id = storyId,
                title = state.title,
                template = state.selectedTemplate,
                startTime = state.startTime,
                endTime = state.endTime,
                coverEmoji = state.coverEmoji,
                createdAt = now,
                updatedAt = now,
            )
            storyRepository.insert(story)

            // Insert manual events
            val manualEvents = state.events.mapIndexed { index, draft ->
                StoryEvent(
                    id = UUID.randomUUID().toString(),
                    storyId = storyId,
                    eventType = draft.eventType,
                    referenceId = draft.referenceId,
                    title = draft.title,
                    description = draft.description,
                    timestamp = draft.timestamp,
                    sortOrder = index,
                    createdAt = now,
                )
            }

            // Insert selected suggested events
            val selectedEvents = state.suggestedEvents
                .filter { it.isSelected }
                .mapIndexed { index, suggested ->
                    StoryEvent(
                        id = UUID.randomUUID().toString(),
                        storyId = storyId,
                        eventType = suggested.eventType,
                        referenceId = suggested.referenceId,
                        title = suggested.title,
                        timestamp = suggested.timestamp,
                        sortOrder = manualEvents.size + index,
                        createdAt = now,
                    )
                }

            val allEvents = manualEvents + selectedEvents
            if (allEvents.isNotEmpty()) {
                storyEventRepository.insertAll(allEvents)
            }

            _uiState.update { it.copy(isSaving = false) }
            onComplete()
        }
    }
}
