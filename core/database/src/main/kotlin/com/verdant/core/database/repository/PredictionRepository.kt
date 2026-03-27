package com.verdant.core.database.repository

import com.verdant.core.model.Prediction
import com.verdant.core.model.PredictionType
import kotlinx.coroutines.flow.Flow

interface PredictionRepository {
    suspend fun getLatestByType(type: PredictionType): Prediction?
    fun observeActive(now: Long): Flow<List<Prediction>>
    suspend fun insert(prediction: Prediction)
    suspend fun deleteExpired(now: Long)
    suspend fun deleteAll()
}
