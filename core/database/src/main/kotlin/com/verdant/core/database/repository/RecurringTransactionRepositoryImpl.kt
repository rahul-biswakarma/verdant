package com.verdant.core.database.repository

import com.verdant.core.database.dao.RecurringTransactionDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecurringTransactionRepositoryImpl @Inject constructor(
    private val dao: RecurringTransactionDao,
) : RecurringTransactionRepository {

    override fun observeActive(): Flow<List<RecurringTransaction>> =
        dao.observeActive().map { it.map { e -> e.toDomain() } }

    override suspend fun getByMerchant(merchant: String): RecurringTransaction? =
        dao.getByMerchant(merchant)?.toDomain()

    override suspend fun insert(recurring: RecurringTransaction) = dao.insert(recurring.toEntity())

    override suspend fun update(recurring: RecurringTransaction) = dao.update(recurring.toEntity())

    override suspend fun deleteAll() = dao.deleteAll()
}
