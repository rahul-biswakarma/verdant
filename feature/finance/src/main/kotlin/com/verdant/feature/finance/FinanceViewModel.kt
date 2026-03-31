package com.verdant.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.common.FinanceDataAggregator
import com.verdant.core.model.CategorySpend
import com.verdant.core.model.RecurringTransaction
import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import com.verdant.core.model.repository.RecurringTransactionRepository
import com.verdant.core.model.repository.TransactionRepository
import com.verdant.core.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val recurringTxnRepository: RecurringTransactionRepository,
    private val aggregator: FinanceDataAggregator,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(FinanceTab.OVERVIEW)
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _searchQuery = MutableStateFlow("")
    private val _filters = MutableStateFlow(TransactionFilters())

    // Current month transactions — reacts to month changes
    @Suppress("OPT_IN_USAGE")
    private val _currentMonthTransactions: Flow<List<Transaction>> =
        _currentMonth.flatMapLatest { month ->
            val (start, end) = monthRange(month)
            transactionRepository.observeByDateRange(start, end)
        }

    // Previous month transactions — for month-over-month comparison
    @Suppress("OPT_IN_USAGE")
    private val _prevMonthTransactions: Flow<List<Transaction>> =
        _currentMonth.flatMapLatest { month ->
            val prev = month.minusMonths(1)
            val (start, end) = monthRange(prev)
            transactionRepository.observeByDateRange(start, end)
        }

    // Last 6 months transactions — for trends charts (re-derives when month changes)
    @Suppress("OPT_IN_USAGE")
    private val _trendsTransactions: Flow<List<Transaction>> =
        _currentMonth.flatMapLatest {
            transactionRepository.observeByDateRange(sixMonthsAgoMs(), nowMs())
        }

    // Pre-combine groups to fit stdlib combine(5)
    private val filterState: Flow<Pair<String, TransactionFilters>> =
        combine(_searchQuery, _filters) { q, f -> q to f }

    private data class TxnData(
        val current: List<Transaction>,
        val previous: List<Transaction>,
        val trends: List<Transaction>,
        val recurring: List<RecurringTransaction>,
    )

    private val txnData: Flow<TxnData> =
        combine(
            _currentMonthTransactions,
            _prevMonthTransactions,
            _trendsTransactions,
            recurringTxnRepository.observeActive(),
        ) { curr, prev, trends, recurring -> TxnData(curr, prev, trends, recurring) }

    private val prefsData: Flow<Pair<Boolean, Boolean>> =
        combine(prefs.smsPermissionGranted, prefs.financeOnboardingCompleted) { s, o -> s to o }

    val uiState: StateFlow<FinanceUiState> = combine(
        _selectedTab,
        _currentMonth,
        prefsData,
        txnData,
        filterState,
    ) { selectedTab, month, (smsGranted, onboarded), txns, (query, filters) ->

        val summary = aggregator.aggregateForMonth(txns.current, month)

        val prevSpent = txns.previous
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }
        val monthOverMonth = if (prevSpent > 0) {
            ((summary.totalSpent - prevSpent) / prevSpent * 100)
        } else {
            null
        }

        val monthlyTotals = aggregator.monthlyTotals(txns.trends, months = 6)
        val categoryHistory = buildCategoryHistory(txns.trends)
        val prediction = aggregator.predictNextMonth(txns.trends, txns.recurring)

        val sorted = txns.current.sortedByDescending { it.transactionDate }
        val filtered = sorted.applyQuery(query).applyFilters(filters)

        FinanceUiState(
            selectedTab = selectedTab,
            currentMonth = month,
            totalSpent = summary.totalSpent,
            totalIncome = summary.totalIncome,
            categoryBreakdown = summary.categoryBreakdown,
            recentTransactions = sorted,
            monthOverMonthChange = monthOverMonth,
            prediction = prediction,
            transactionCount = txns.current.size,
            searchQuery = query,
            activeFilters = filters,
            filteredTransactions = filtered,
            monthlyTotals = monthlyTotals,
            categoryHistory = categoryHistory,
            smsPermissionGranted = smsGranted,
            financeOnboardingCompleted = onboarded,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState(),
    )

    // ── Actions ──────────────────────────────────────────────────

    fun selectTab(tab: FinanceTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilters(filters: TransactionFilters) {
        _filters.value = filters
    }

    fun setMonth(month: YearMonth) {
        _currentMonth.value = month
    }

    fun completeSmsOnboarding() = viewModelScope.launch {
        prefs.setSmsPermissionGranted(true)
        prefs.setFinanceOnboardingCompleted(true)
    }

    fun completeFinanceOnboarding() = viewModelScope.launch {
        prefs.setFinanceOnboardingCompleted(true)
    }

    fun saveTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.insert(transaction)
    }

    fun updateTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.update(transaction)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.delete(transaction)
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun buildCategoryHistory(
        txns: List<Transaction>,
    ): Map<String, List<CategorySpend>> {
        val zone = ZoneId.systemDefault()
        return txns.groupBy { txn ->
            val date = Instant.ofEpochMilli(txn.transactionDate).atZone(zone).toLocalDate()
            YearMonth.from(date).toString()
        }.mapValues { (monthStr, monthTxns) ->
            val month = YearMonth.parse(monthStr)
            aggregator.aggregateForMonth(monthTxns, month).categoryBreakdown
        }
    }

    private fun List<Transaction>.applyQuery(query: String): List<Transaction> {
        if (query.isBlank()) return this
        val q = query.trim().lowercase()
        return filter {
            it.merchant?.lowercase()?.contains(q) == true ||
                it.category?.lowercase()?.contains(q) == true
        }
    }

    private fun List<Transaction>.applyFilters(filters: TransactionFilters): List<Transaction> {
        var list = this
        filters.type?.let { t -> list = list.filter { it.type == t } }
        filters.category?.let { c -> list = list.filter { it.category == c } }
        filters.dateRange?.let { r -> list = list.filter { it.transactionDate in r } }
        return list
    }

    private fun monthRange(month: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    private fun sixMonthsAgoMs(): Long {
        val zone = ZoneId.systemDefault()
        return YearMonth.now().minusMonths(5).atDay(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun nowMs(): Long = System.currentTimeMillis()
}
