package com.verdant.feature.stories

import com.verdant.core.model.StoryTemplate

data class StoryListUiState(
    val isLoading: Boolean = true,
    val stories: List<StoryListItem> = emptyList(),
)

data class StoryListItem(
    val id: String,
    val title: String,
    val coverEmoji: String,
    val template: StoryTemplate?,
    val startTime: Long,
    val endTime: Long?,
    val eventCount: Int,
    val hasSummary: Boolean,
)
