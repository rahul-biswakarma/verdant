package com.verdant.feature.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.model.repository.StoryEventRepository
import com.verdant.core.model.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoryListViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val storyEventRepository: StoryEventRepository,
) : ViewModel() {

    val uiState: StateFlow<StoryListUiState> = storyRepository.observeAll()
        .map { stories ->
            StoryListUiState(
                isLoading = false,
                stories = stories.map { story ->
                    val eventCount = storyEventRepository.getByStoryId(story.id).size
                    StoryListItem(
                        id = story.id,
                        title = story.title,
                        coverEmoji = story.coverEmoji,
                        template = story.template,
                        startTime = story.startTime,
                        endTime = story.endTime,
                        eventCount = eventCount,
                        hasSummary = story.aiSummary != null,
                    )
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StoryListUiState(),
        )

    fun deleteStory(id: String) {
        viewModelScope.launch {
            storyRepository.delete(id)
        }
    }
}
