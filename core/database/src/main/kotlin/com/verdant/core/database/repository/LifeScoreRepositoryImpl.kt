package com.verdant.core.database.repository

import com.verdant.core.database.dao.LifeScoreDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.LifeScore
import com.verdant.core.model.ScoreType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LifeScoreRepositoryImpl @Inject constructor(
    private val dao: LifeScoreDao,
) : LifeScoreRepository {

    override suspend fun getLatestByType(type: ScoreType): LifeScore? =
        dao.getLatestByType(type.name)?.toDomain()

    override fun observeByRange(start: Long, end: Long): Flow<List<LifeScore>> =
        dao.observeByRange(start, end).map { it.map { e -> e.toDomain() } }

    override fun observeTrendByType(type: ScoreType, start: Long, end: Long): Flow<List<LifeScore>> =
        dao.observeTrendByType(type.name, start, end).map { it.map { e -> e.toDomain() } }

    override suspend fun insert(score: LifeScore) = dao.insert(score.toEntity())

    override suspend fun insertAll(scores: List<LifeScore>) = dao.insertAll(scores.map { it.toEntity() })

    override suspend fun deleteOlderThan(before: Long) = dao.deleteOlderThan(before)

    override suspend fun deleteAll() = dao.deleteAll()
}
