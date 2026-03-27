package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.database.dao.BudgetDao
import com.verdant.core.database.dao.TransactionDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_budget_alert"
    }

    override suspend fun doWork(): Result {
        // TODO: Check each active budget against actual spending
        // 1. Load all budgets
        // 2. For each budget, sum transactions in the current period
        // 3. If spending > 80% of budget, create notification
        // 4. If spending > 100%, create urgent notification
        return Result.success()
    }
}
