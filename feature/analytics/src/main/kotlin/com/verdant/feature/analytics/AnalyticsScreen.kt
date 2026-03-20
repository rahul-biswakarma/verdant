package com.verdant.feature.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.feature.analytics.tab.CorrelationsTab
import com.verdant.feature.analytics.tab.HeatmapsTab
import com.verdant.feature.analytics.tab.OverviewTab
import com.verdant.feature.analytics.tab.ReportsTab
import com.verdant.feature.analytics.tab.TrendsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Analytics") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        // Scrollable tab row — 5 tabs fit nicely on most screens when scrollable
        ScrollableTabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnalyticsTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick  = { viewModel.selectTab(tab) },
                    text     = { Text(tab.label) },
                )
            }
        }

        // Tab content
        when (state.selectedTab) {
            AnalyticsTab.OVERVIEW      -> OverviewTab(state = state.overview)
            AnalyticsTab.HEATMAPS      -> HeatmapsTab(
                habits          = state.habits,
                state           = state.heatmaps,
                onHabitSelected = viewModel::selectHabitForHeatmap,
            )
            AnalyticsTab.TRENDS        -> TrendsTab(
                state           = state.trends,
                onSeriesSelected = viewModel::selectTrendSeries,
            )
            AnalyticsTab.CORRELATIONS  -> CorrelationsTab(
                habits      = state.habits,
                state       = state.correlations,
                onGenerate  = viewModel::generateCorrelations,
            )
            AnalyticsTab.REPORTS       -> ReportsTab(
                state            = state.reports,
                onGenerateWeekly  = viewModel::generateWeeklyReport,
                onGenerateMonthly = viewModel::generateMonthlyReport,
                onToggleExpand    = viewModel::toggleReportExpanded,
            )
        }
    }
}
