package com.verdant.feature.finance

import com.verdant.core.model.CategorySpend
import com.verdant.core.model.MonthlyPrediction
import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import java.time.YearMonth

data class FinanceUiState(
    val selectedTab: FinanceTab = FinanceTab.OVERVIEW,
    val currentMonth: YearMonth = YearMonth.now(),
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val monthOverMonthChange: Double? = null,
    val prediction: MonthlyPrediction? = null,
    val transactionCount: Int = 0,
    val searchQuery: String = "",
    val activeFilters: TransactionFilters = TransactionFilters(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val monthlyTotals: Map<String, Double> = emptyMap(),
    val categoryHistory: Map<String, List<CategorySpend>> = emptyMap(),
    val smsPermissionGranted: Boolean = false,
    val financeOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = true,
)

data class TransactionFilters(
    val dateRange: ClosedRange<Long>? = null,
    val type: TransactionType? = null,
    val category: String? = null,
)

enum class FinanceTab(val label: String) {
    OVERVIEW("Overview"),
    TRANSACTIONS("Transactions"),
    TRENDS("Trends"),
}
