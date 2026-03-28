package com.verdant.core.model.repository

import com.verdant.core.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun observeActive(): Flow<List<RecurringTransaction>>
    suspend fun getByMerchant(merchant: String): RecurringTransaction?
    suspend fun insert(recurring: RecurringTransaction)
    suspend fun update(recurring: RecurringTransaction)
    suspend fun deleteAll()
}
