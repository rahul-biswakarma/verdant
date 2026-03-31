package com.verdant.feature.stories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.ai.StoryAnalysisContext
import com.verdant.core.ai.StoryEventSummary
import com.verdant.core.ai.VerdantAI
import com.verdant.core.model.repository.StoryEventRepository
import com.verdant.core.model.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storyRepository: StoryRepository,
    private val storyEventRepository: StoryEventRepository,
    private val verdantAI: VerdantAI,
) : ViewModel() {

    private val storyId: String = savedStateHandle["storyId"] ?: ""

    private val _analyzeState = MutableStateFlow(AnalyzeState())

    private data class AnalyzeState(
        val isAnalyzing: Boolean = false,
        val error: String? = null,
    )

    val uiState: StateFlow<StoryDetailUiState> = combine(
        storyEventRepository.observeByStoryId(storyId),
        _analyzeState,
    ) { events, analyzeState ->
        val story = storyRepository.getById(storyId)
        StoryDetailUiState(
            isLoading = false,
            story = story,
            events = events,
            isAnalyzing = analyzeState.isAnalyzing,
            analyzeError = analyzeState.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StoryDetailUiState(),
    )

    fun analyzeStory() {
        viewModelScope.launch {
            val story = storyRepository.getById(storyId) ?: return@launch
            val events = storyEventRepository.getByStoryId(storyId)

            _analyzeState.value = AnalyzeState(isAnalyzing = true)

            try {
                val endTime = story.endTime
                val duration = if (endTime != null) {
                    (endTime - story.startTime) / 60_000
                } else {
                    (System.currentTimeMillis() - story.startTime) / 60_000
                }

                val context = StoryAnalysisContext(
                    storyTitle = story.title,
                    template = story.template?.name,
                    events = events.map { event ->
                        StoryEventSummary(
                            type = event.eventType.name,
                            title = event.title,
                            timestamp = event.timestamp,
                            metadata = event.metadata,
                        )
                    },
                    durationMinutes = duration,
                )

                val result = verdantAI.analyzeStory(context)

                // Update story with AI results
                storyRepository.update(
                    story.copy(
                        aiSummary = result.summary,
                        aiInsights = result.behavioralInsights.joinToString("\n") +
                            "\n\n" + result.patterns.joinToString("\n") +
                            "\n\n" + result.suggestedActions.joinToString("\n"),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )

                _analyzeState.value = AnalyzeState()
            } catch (e: Exception) {
                _analyzeState.value = AnalyzeState(error = e.message ?: "Analysis failed")
            }
        }
    }

    fun deleteStory() {
        viewModelScope.launch {
            storyRepository.delete(storyId)
        }
    }
}
