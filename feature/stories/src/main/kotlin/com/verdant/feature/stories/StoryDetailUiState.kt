package com.verdant.feature.stories

import com.verdant.core.model.Story
import com.verdant.core.model.StoryEvent

data class StoryDetailUiState(
    val isLoading: Boolean = true,
    val story: Story? = null,
    val events: List<StoryEvent> = emptyList(),
    val isAnalyzing: Boolean = false,
    val analyzeError: String? = null,
)
