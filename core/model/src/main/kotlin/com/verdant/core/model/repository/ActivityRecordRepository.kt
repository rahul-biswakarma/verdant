package com.verdant.core.model.repository

import com.verdant.core.model.ActivityRecord
import com.verdant.core.model.ActivityType
import kotlinx.coroutines.flow.Flow

interface ActivityRecordRepository {
    suspend fun insert(record: ActivityRecord)
    suspend fun insertAll(records: List<ActivityRecord>)
    fun observeByRange(start: Long, end: Long): Flow<List<ActivityRecord>>
    suspend fun getLatestByType(type: ActivityType): ActivityRecord?
    suspend fun deleteOlderThan(before: Long)
    suspend fun deleteAll()
}
