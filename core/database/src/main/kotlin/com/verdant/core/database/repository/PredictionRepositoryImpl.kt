package com.verdant.core.database.repository

import com.verdant.core.database.dao.PredictionDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.Prediction
import com.verdant.core.model.PredictionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PredictionRepositoryImpl @Inject constructor(
    private val dao: PredictionDao,
) : PredictionRepository {

    override suspend fun getLatestByType(type: PredictionType): Prediction? =
        dao.getLatestByType(type.name)?.toDomain()

    override fun observeActive(now: Long): Flow<List<Prediction>> =
        dao.observeActive(now).map { it.map { e -> e.toDomain() } }

    override suspend fun insert(prediction: Prediction) = dao.insert(prediction.toEntity())

    override suspend fun deleteExpired(now: Long) = dao.deleteExpired(now)

    override suspend fun deleteAll() = dao.deleteAll()
}
