package com.verdant.core.database.repository

import com.verdant.core.database.dao.PlayerProfileDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlayerProfileRepositoryImpl @Inject constructor(
    private val dao: PlayerProfileDao,
) : PlayerProfileRepository {

    override suspend fun get(): PlayerProfile? = dao.get()?.toDomain()

    override fun observe(): Flow<PlayerProfile?> = dao.observe().map { it?.toDomain() }

    override suspend fun insert(profile: PlayerProfile) = dao.insert(profile.toEntity())

    override suspend fun update(profile: PlayerProfile) = dao.update(profile.toEntity())

    override suspend fun deleteAll() = dao.deleteAll()
}
