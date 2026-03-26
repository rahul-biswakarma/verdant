package com.verdant.feature.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.feature.finance.onboarding.FinanceOnboardingContent
import com.verdant.feature.finance.tab.OverviewTab
import com.verdant.feature.finance.tab.TransactionsTab
import com.verdant.feature.finance.tab.TrendsTab

@Composable
fun FinanceScreen(
    onNavigateToTransactionDetail: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!state.financeOnboardingCompleted) {
            FinanceOnboardingContent()
            return@Surface
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Text(
                text = "Finance",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            )

            // Tab Row
            val tabs = FinanceTab.entries
            val selectedIndex = tabs.indexOf(state.selectedTab)
            TabRow(selectedTabIndex = selectedIndex) {
                tabs.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { /* update selected tab via viewmodel */ },
                        text = { Text(tab.label) },
                    )
                }
            }

            // Tab Content
            when (state.selectedTab) {
                FinanceTab.OVERVIEW -> OverviewTab(
                    totalSpent = state.totalSpent,
                    totalIncome = state.totalIncome,
                    categoryBreakdown = state.categoryBreakdown,
                    prediction = state.prediction,
                    monthOverMonthChange = state.monthOverMonthChange,
                )
                FinanceTab.TRANSACTIONS -> TransactionsTab(
                    transactions = state.recentTransactions,
                    onTransactionClick = onNavigateToTransactionDetail,
                )
                FinanceTab.TRENDS -> TrendsTab()
            }
        }
    }
}
