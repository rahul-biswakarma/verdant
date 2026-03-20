package com.verdant.feature.insights

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text("AI Insights") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        // Two-tab row: Feed | Coach
        TabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth(),
        ) {
            InsightsTab.entries.forEachIndexed { _, tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick  = { viewModel.selectTab(tab) },
                    text     = { Text(tab.label) },
                )
            }
        }

        when (state.selectedTab) {
            InsightsTab.FEED  -> InsightsFeedTab(
                state     = state.feed,
                onDismiss = viewModel::dismissInsight,
            )
            InsightsTab.COACH -> CoachChatTab(
                state          = state.chat,
                onInputChanged = viewModel::onInputChanged,
                onSend         = viewModel::sendMessage,
                onRetry        = viewModel::retryLastMessage,
                onClear        = viewModel::clearChat,
            )
        }
    }
}
