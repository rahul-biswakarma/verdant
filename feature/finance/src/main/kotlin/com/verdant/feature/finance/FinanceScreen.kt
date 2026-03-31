package com.verdant.feature.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    selectedTab: FinanceTab = FinanceTab.OVERVIEW,
    onNavigateToTransactionDetail: (String) -> Unit = {},
    onNavigateToCreateTransaction: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!state.financeOnboardingCompleted) {
            FinanceOnboardingContent(
                onPermissionGranted = { viewModel.completeSmsOnboarding() },
                onSkip = { viewModel.completeFinanceOnboarding() },
            )
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

            // Tab Content
            when (selectedTab) {
                FinanceTab.OVERVIEW -> OverviewTab(
                    totalSpent = state.totalSpent,
                    totalIncome = state.totalIncome,
                    categoryBreakdown = state.categoryBreakdown,
                    prediction = state.prediction,
                    monthOverMonthChange = state.monthOverMonthChange,
                )
                FinanceTab.TRANSACTIONS -> TransactionsTab(
                    transactions = state.filteredTransactions,
                    searchQuery = state.searchQuery,
                    activeFilters = state.activeFilters,
                    onTransactionClick = onNavigateToTransactionDetail,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onFiltersChange = viewModel::setFilters,
                    onCreateTransaction = onNavigateToCreateTransaction,
                )
                FinanceTab.TRENDS -> TrendsTab(
                    monthlyTotals = state.monthlyTotals,
                    categoryBreakdown = state.categoryBreakdown,
                    monthOverMonthChange = state.monthOverMonthChange,
                )
            }
        }
    }
}
