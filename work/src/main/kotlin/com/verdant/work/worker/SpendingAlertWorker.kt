package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.model.repository.TransactionRepository
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.work.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.YearMonth
import java.time.ZoneId

/**
 * Checks for unusual spending patterns and budget overruns.
 * Runs every 6 hours when finance alerts are enabled.
 */
@HiltWorker
class SpendingAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.financeAlertsEnabled.first()) return Result.success()
        if (!prefs.smsPermissionGranted.first()) return Result.success()

        val zone = ZoneId.systemDefault()
        val month = YearMonth.now()
        val startMs = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val totalSpent = transactionRepository.totalSpent(startMs, endMs).firstOrNull() ?: 0.0

        // Check last month for comparison
        val prevMonth = month.minusMonths(1)
        val prevStart = prevMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val prevEnd = prevMonth.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val lastMonthTotal = transactionRepository.totalSpent(prevStart, prevEnd).firstOrNull() ?: 0.0

        // Alert if spending exceeds last month's total before month end
        if (lastMonthTotal > 0 && totalSpent > lastMonthTotal) {
            NotificationHelper.postFinanceAlert(
                context = applicationContext,
                title = "Spending exceeds last month",
                message = "You've spent more this month than all of last month. Consider reviewing your expenses.",
            )
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "verdant_spending_alert"
    }
}
