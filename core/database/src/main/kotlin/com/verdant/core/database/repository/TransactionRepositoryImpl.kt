package com.verdant.core.database.repository

import com.verdant.core.database.dao.TransactionDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeByDateRange(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.observeByDateRange(start, end).map { entities -> entities.map { it.toDomain() } }

    override fun totalSpent(start: Long, end: Long): Flow<Double?> =
        transactionDao.totalSpent(start, end)

    override fun totalIncome(start: Long, end: Long): Flow<Double?> =
        transactionDao.totalIncome(start, end)

    override suspend fun getById(id: String): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun getByRawSmsId(smsId: Long): Transaction? =
        transactionDao.getByRawSmsId(smsId)?.toDomain()

    override suspend fun insert(transaction: Transaction) =
        transactionDao.insert(transaction.toEntity())

    override suspend fun insertAll(transactions: List<Transaction>) =
        transactionDao.insertAll(transactions.map { it.toEntity() })

    override suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction.toEntity())

    override suspend fun delete(transaction: Transaction) =
        transactionDao.delete(transaction.toEntity())

    override suspend fun deleteAll() =
        transactionDao.deleteAll()

    override suspend fun count(): Int =
        transactionDao.count()
}
