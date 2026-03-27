package com.verdant.core.database.repository

import com.verdant.core.database.dao.HealthRecordDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.HealthRecord
import com.verdant.core.model.HealthRecordType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HealthRecordRepositoryImpl @Inject constructor(
    private val dao: HealthRecordDao,
) : HealthRecordRepository {

    override fun observeByTypeAndRange(type: HealthRecordType, start: Long, end: Long): Flow<List<HealthRecord>> =
        dao.observeByTypeAndRange(type.name, start, end).map { it.map { e -> e.toDomain() } }

    override fun observeByRange(start: Long, end: Long): Flow<List<HealthRecord>> =
        dao.observeByRange(start, end).map { it.map { e -> e.toDomain() } }

    override suspend fun getLatestByType(type: HealthRecordType): HealthRecord? =
        dao.getLatestByType(type.name)?.toDomain()

    override suspend fun insert(record: HealthRecord) = dao.insert(record.toEntity())

    override suspend fun insertAll(records: List<HealthRecord>) = dao.insertAll(records.map { it.toEntity() })

    override suspend fun deleteOlderThan(before: Long) = dao.deleteOlderThan(before)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun count(): Int = dao.count()
}
