package com.verdant.core.ai

/**
 * Compact context sent to Claude for story analysis. Contains the story
 * metadata and a summary of each linked event.
 */
data class StoryAnalysisContext(
    val storyTitle: String,
    val template: String?,
    val events: List<StoryEventSummary>,
    val durationMinutes: Long,
)

data class StoryEventSummary(
    val type: String,
    val title: String,
    val timestamp: Long,
    val metadata: String?,
)

data class StoryAnalysisResult(
    val summary: String,
    val behavioralInsights: List<String>,
    val patterns: List<String>,
    val suggestedActions: List<String>,
)
