package com.verdant.feature.insights

import com.verdant.core.model.AIInsight
import com.verdant.core.model.ChatState
import com.verdant.core.model.InsightType

enum class InsightsTab(val label: String) {
    FEED("Feed"),
    COACH("Coach"),
}
data class InsightsUiState(
    val selectedTab: InsightsTab = InsightsTab.FEED,
    val feed: FeedState = FeedState(),
    val chat: ChatState = ChatState(),
)
data class FeedState(
    val isLoading: Boolean = true,
    val insights: List<AIInsight> = emptyList(),
    val isEmpty: Boolean = false,
)
