package com.verdant.core.model.repository

import com.verdant.core.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeActive(): Flow<List<Budget>>
    suspend fun getByCategory(category: String): Budget?
    suspend fun getById(id: String): Budget?
    suspend fun insert(budget: Budget)
    suspend fun update(budget: Budget)
    suspend fun delete(budget: Budget)
    suspend fun deleteAll()
}
