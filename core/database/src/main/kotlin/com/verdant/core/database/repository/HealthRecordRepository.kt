package com.verdant.core.database.repository

import com.verdant.core.model.HealthRecord
import com.verdant.core.model.HealthRecordType
import kotlinx.coroutines.flow.Flow

interface HealthRecordRepository {
    fun observeByTypeAndRange(type: HealthRecordType, start: Long, end: Long): Flow<List<HealthRecord>>
    fun observeByRange(start: Long, end: Long): Flow<List<HealthRecord>>
    suspend fun getLatestByType(type: HealthRecordType): HealthRecord?
    suspend fun insert(record: HealthRecord)
    suspend fun insertAll(records: List<HealthRecord>)
    suspend fun deleteOlderThan(before: Long)
    suspend fun deleteAll()
    suspend fun count(): Int
}
