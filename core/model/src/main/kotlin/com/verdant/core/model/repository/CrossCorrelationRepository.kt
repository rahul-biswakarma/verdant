package com.verdant.core.model.repository

import com.verdant.core.model.CrossCorrelation
import kotlinx.coroutines.flow.Flow

interface CrossCorrelationRepository {
    suspend fun insert(correlation: CrossCorrelation)
    suspend fun insertAll(correlations: List<CrossCorrelation>)
    fun observeAll(): Flow<List<CrossCorrelation>>
    fun observeSignificant(minStrength: Float): Flow<List<CrossCorrelation>>
    suspend fun deleteAll()
}
