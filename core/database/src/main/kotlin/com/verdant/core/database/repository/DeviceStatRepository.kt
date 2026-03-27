package com.verdant.core.database.repository

import com.verdant.core.model.DeviceStat
import com.verdant.core.model.DeviceStatType
import kotlinx.coroutines.flow.Flow

interface DeviceStatRepository {
    fun observeByTypeAndRange(type: DeviceStatType, start: Long, end: Long): Flow<List<DeviceStat>>
    fun observeByRange(start: Long, end: Long): Flow<List<DeviceStat>>
    suspend fun getLatestByType(type: DeviceStatType): DeviceStat?
    suspend fun insert(stat: DeviceStat)
    suspend fun insertAll(stats: List<DeviceStat>)
    suspend fun deleteOlderThan(before: Long)
    suspend fun deleteAll()
    suspend fun count(): Int
}
