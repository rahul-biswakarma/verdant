package com.verdant.core.model.repository

import com.verdant.core.model.DeviceSignal
import kotlinx.coroutines.flow.Flow

interface DeviceSignalRepository {
    suspend fun insert(signal: DeviceSignal)
    suspend fun insertAll(signals: List<DeviceSignal>)
    fun observeByDeviceAndRange(deviceId: String, start: Long, end: Long): Flow<List<DeviceSignal>>
    fun observeByRange(start: Long, end: Long): Flow<List<DeviceSignal>>
    suspend fun deleteOlderThan(before: Long)
    suspend fun deleteAll()
}
