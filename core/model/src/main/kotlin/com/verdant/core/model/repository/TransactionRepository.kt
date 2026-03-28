package com.verdant.core.model.repository

import com.verdant.core.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeByDateRange(start: Long, end: Long): Flow<List<Transaction>>
    fun totalSpent(start: Long, end: Long): Flow<Double?>
    fun totalIncome(start: Long, end: Long): Flow<Double?>
    suspend fun getById(id: String): Transaction?
    suspend fun getByRawSmsId(smsId: Long): Transaction?
    suspend fun insert(transaction: Transaction)
    suspend fun insertAll(transactions: List<Transaction>)
    suspend fun update(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    suspend fun deleteAll()
    suspend fun count(): Int
}
