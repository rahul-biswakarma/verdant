package com.verdant.core.database.repository

import com.verdant.core.database.dao.BudgetDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao,
) : BudgetRepository {

    override fun observeActive(): Flow<List<Budget>> =
        dao.observeActive().map { it.map { e -> e.toDomain() } }

    override suspend fun getByCategory(category: String): Budget? =
        dao.getByCategory(category)?.toDomain()

    override suspend fun getById(id: String): Budget? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(budget: Budget) = dao.insert(budget.toEntity())

    override suspend fun update(budget: Budget) = dao.update(budget.toEntity())

    override suspend fun delete(budget: Budget) = dao.delete(budget.toEntity())

    override suspend fun deleteAll() = dao.deleteAll()
}
