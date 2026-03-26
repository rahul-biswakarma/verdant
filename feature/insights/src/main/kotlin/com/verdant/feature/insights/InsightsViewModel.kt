package com.verdant.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.ai.AIFeatureUnavailableException
import com.verdant.core.ai.HabitSummary
import com.verdant.core.ai.VerdantAI
import com.verdant.core.model.ChatBubble
import com.verdant.core.model.ChatState
import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.model.ChatMessage
import com.verdant.core.model.isScheduledForDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightDao: AIInsightDao,
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val verdantAI: VerdantAI,
) : ViewModel() {

    private val _state = MutableStateFlow(InsightsUiState())
    val state: StateFlow<InsightsUiState> = _state

    init {
        observeFeed()
    }


    fun selectTab(tab: InsightsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }


    private fun observeFeed() {
        viewModelScope.launch {
            insightDao.observeRecent(limit = 50).collect { entities ->
                val insights = entities.map { e ->
                    com.verdant.core.model.AIInsight(
                        id             = e.id,
                        type           = e.type,
                        content        = e.content,
                        relatedHabitIds = e.relatedHabitIds,
                        generatedAt    = e.generatedAt,
                        expiresAt      = e.expiresAt,
                        dismissed      = e.dismissed,
                    )
                }
                _state.update { s ->
                    s.copy(
                        feed = FeedState(
                            isLoading = false,
                            insights  = insights,
                            isEmpty   = insights.isEmpty(),
                        )
                    )
                }
            }
        }
    }

    fun dismissInsight(id: String) {
        viewModelScope.launch {
            insightDao.dismiss(id)
            // The observeRecent flow will automatically re-emit without this insight
        }
    }


    fun onInputChanged(text: String) {
        _state.update { it.copy(chat = it.chat.copy(inputText = text, sendError = null)) }
    }

    fun sendMessage() {
        val text = _state.value.chat.inputText.trim()
        if (text.isBlank() || _state.value.chat.isSending) return

        val userBubble = ChatBubble(
            id        = UUID.randomUUID().toString(),
            role      = "user",
            content   = text,
            timestamp = System.currentTimeMillis(),
        )

        // Add user message + streaming placeholder + clear input
        val streamingId = UUID.randomUUID().toString()
        val streamingBubble = ChatBubble(
            id          = streamingId,
            role        = "assistant",
            content     = "",
            timestamp   = System.currentTimeMillis(),
            isStreaming = true,
        )

        _state.update { s ->
            s.copy(
                chat = s.chat.copy(
                    messages  = s.chat.messages + userBubble + streamingBubble,
                    inputText = "",
                    isSending = true,
                    sendError = null,
                )
            )
        }

        viewModelScope.launch {
            val result = runCatching { callCoach(text) }

            result.fold(
                onSuccess = { reply ->
                    _state.update { s ->
                        s.copy(
                            chat = s.chat.copy(
                                messages = s.chat.messages.map { bubble ->
                                    if (bubble.id == streamingId) {
                                        bubble.copy(content = reply, isStreaming = false)
                                    } else bubble
                                },
                                isSending = false,
                            )
                        )
                    }
                },
                onFailure = { e ->
                    val errorMsg = when (e) {
                        is AIFeatureUnavailableException -> when (e.reason) {
                            AIFeatureUnavailableException.Reason.NO_NETWORK ->
                                "Connect to the internet to chat with your coach"
                            AIFeatureUnavailableException.Reason.RATE_LIMITED ->
                                "Daily chat limit reached — try again tomorrow"
                            AIFeatureUnavailableException.Reason.AUTH_ERROR ->
                                "Please sign in to use the AI coach"
                            else -> "Something went wrong — please try again"
                        }
                        else -> "Something went wrong — please try again"
                    }

                    _state.update { s ->
                        s.copy(
                            chat = s.chat.copy(
                                messages = s.chat.messages.map { bubble ->
                                    if (bubble.id == streamingId) {
                                        bubble.copy(
                                            content     = errorMsg,
                                            isStreaming = false,
                                            isError     = true,
                                        )
                                    } else bubble
                                },
                                isSending = false,
                                sendError = null, // error is shown inline in the bubble
                            )
                        )
                    }
                }
            )
        }
    }

    fun retryLastMessage() {
        val messages = _state.value.chat.messages
        // Find the last user message before the error bubble
        val lastUserBubble = messages.lastOrNull { it.role == "user" } ?: return
        // Remove the error bubble
        _state.update { s ->
            s.copy(
                chat = s.chat.copy(
                    messages  = s.chat.messages.dropLastWhile { it.isError || it.isStreaming },
                    inputText = lastUserBubble.content,
                )
            )
        }
        // Also remove the user bubble we just re-added to input
        _state.update { s ->
            s.copy(
                chat = s.chat.copy(
                    messages = s.chat.messages.dropLastWhile { it.id == lastUserBubble.id },
                )
            )
        }
        sendMessage()
    }

    fun clearChat() {
        _state.update { it.copy(chat = ChatState()) }
    }


    private suspend fun callCoach(userMessage: String): String {
        val habits  = habitRepository.observeActiveHabits().first()
        val today   = LocalDate.now()
        val start7d = today.minusDays(6)

        val entries7d   = entryRepository.observeAllEntries(start7d, today).first()
        val streaks      = calculateStreak.currentStreaks(habits.map { it.id })
        val completedThisWeek = entries7d.count { it.completed }
        val scheduledThisWeek = habits.sumOf { habit ->
            (0 until 7).count { offset ->
                habit.isScheduledForDate(today.minusDays(offset.toLong()))
            }
        }
        val weekRate = if (scheduledThisWeek == 0) 0f
                       else completedThisWeek.toFloat() / scheduledThisWeek

        val habitSummary = HabitSummary(
            habits              = habits,
            recentCompletionRate = weekRate,
            activeStreaks        = streaks.filterValues { it > 0 },
            periodDays          = 7,
        )

        // Build conversation history: all non-error, non-streaming bubbles
        val history = _state.value.chat.messages
            .filter { !it.isError && !it.isStreaming }
            .map { bubble ->
                ChatMessage(
                    id           = bubble.id,
                    role         = bubble.role,
                    content      = bubble.content,
                    habitContext = null,
                    timestamp    = bubble.timestamp,
                )
            }

        return verdantAI.chatWithCoach(history, habitSummary)
    }

}
