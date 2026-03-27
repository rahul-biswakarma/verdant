package com.verdant.core.database.repository

import com.verdant.core.database.dao.QuestDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.Quest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestRepositoryImpl @Inject constructor(
    private val dao: QuestDao,
) : QuestRepository {

    override fun observeActive(): Flow<List<Quest>> =
        dao.observeActive().map { it.map { e -> e.toDomain() } }

    override fun observeCompleted(): Flow<List<Quest>> =
        dao.observeCompleted().map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): Quest? = dao.getById(id)?.toDomain()

    override suspend fun insert(quest: Quest) = dao.insert(quest.toEntity())

    override suspend fun update(quest: Quest) = dao.update(quest.toEntity())

    override suspend fun start(id: String, startedAt: Long) =
        dao.updateStatus(id, "ACTIVE", startedAt)

    override suspend fun complete(id: String, completedAt: Long) =
        dao.complete(id, completedAt)

    override suspend fun deleteExpired() = dao.deleteExpired()

    override suspend fun deleteAll() = dao.deleteAll()
}
