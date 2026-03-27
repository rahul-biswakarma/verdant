package com.verdant.core.database.repository

import com.verdant.core.database.dao.EmotionalContextDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.EmotionalContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EmotionalContextRepositoryImpl @Inject constructor(
    private val dao: EmotionalContextDao,
) : EmotionalContextRepository {

    override suspend fun getLatest(): EmotionalContext? = dao.getLatest()?.toDomain()

    override fun observeLatest(): Flow<EmotionalContext?> =
        dao.observeLatest().map { it?.toDomain() }

    override fun observeByRange(start: Long, end: Long): Flow<List<EmotionalContext>> =
        dao.observeByRange(start, end).map { it.map { e -> e.toDomain() } }

    override suspend fun insert(context: EmotionalContext) = dao.insert(context.toEntity())

    override suspend fun update(context: EmotionalContext) = dao.update(context.toEntity())

    override suspend fun deleteOlderThan(before: Long) = dao.deleteOlderThan(before)

    override suspend fun deleteAll() = dao.deleteAll()
}
