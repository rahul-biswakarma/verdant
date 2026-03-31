package com.verdant.feature.stories

import com.verdant.core.model.StoryEventType
import com.verdant.core.model.StoryTemplate

data class CreateStoryUiState(
    val title: String = "",
    val selectedTemplate: StoryTemplate? = null,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val coverEmoji: String = "\uD83D\uDCD6",
    val events: List<DraftEvent> = emptyList(),
    val suggestedEvents: List<SuggestedEvent> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val isSaving: Boolean = false,
)

data class DraftEvent(
    val eventType: StoryEventType,
    val title: String,
    val description: String? = null,
    val timestamp: Long,
    val referenceId: String? = null,
)

data class SuggestedEvent(
    val eventType: StoryEventType,
    val title: String,
    val referenceId: String,
    val timestamp: Long,
    val isSelected: Boolean = false,
)
