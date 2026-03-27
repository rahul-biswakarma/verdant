package com.verdant.core.database.repository

import com.verdant.core.database.dao.DeviceStatDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.DeviceStat
import com.verdant.core.model.DeviceStatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeviceStatRepositoryImpl @Inject constructor(
    private val dao: DeviceStatDao,
) : DeviceStatRepository {

    override fun observeByTypeAndRange(type: DeviceStatType, start: Long, end: Long): Flow<List<DeviceStat>> =
        dao.observeByTypeAndRange(type.name, start, end).map { it.map { e -> e.toDomain() } }

    override fun observeByRange(start: Long, end: Long): Flow<List<DeviceStat>> =
        dao.observeByRange(start, end).map { it.map { e -> e.toDomain() } }

    override suspend fun getLatestByType(type: DeviceStatType): DeviceStat? =
        dao.getLatestByType(type.name)?.toDomain()

    override suspend fun insert(stat: DeviceStat) = dao.insert(stat.toEntity())

    override suspend fun insertAll(stats: List<DeviceStat>) = dao.insertAll(stats.map { it.toEntity() })

    override suspend fun deleteOlderThan(before: Long) = dao.deleteOlderThan(before)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun count(): Int = dao.count()
}
