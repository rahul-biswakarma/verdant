package com.verdant.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.model.repository.TransactionRepository
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.model.CategorySpend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    private val currentMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<FinanceUiState> = combine(
        currentMonth,
        prefs.smsPermissionGranted,
        prefs.financeOnboardingCompleted,
    ) { month, smsGranted, onboarded ->
        val (startMs, endMs) = monthRange(month)

        // Get transactions for current month as a snapshot
        val spent = transactionRepository.totalSpent(startMs, endMs)
        val income = transactionRepository.totalIncome(startMs, endMs)

        FinanceUiState(
            currentMonth = month,
            smsPermissionGranted = smsGranted,
            financeOnboardingCompleted = onboarded,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState(),
    )

    private fun monthRange(month: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    private fun LocalDate.plusDays(days: Long): LocalDate = this.plusDays(days)
}
