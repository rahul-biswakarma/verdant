package com.verdant.feature.finance

import com.verdant.core.model.CategorySpend
import com.verdant.core.model.MonthlyPrediction
import com.verdant.core.model.Transaction
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
    val smsPermissionGranted: Boolean = false,
    val financeOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = true,
)

enum class FinanceTab(val label: String) {
    OVERVIEW("Overview"),
    TRANSACTIONS("Transactions"),
    TRENDS("Trends"),
}
