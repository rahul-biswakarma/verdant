package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.DeviceSignalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceSignalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(signal: DeviceSignalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(signals: List<DeviceSignalEntity>)

    @Query("SELECT * FROM device_signals WHERE device_id = :deviceId AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun observeByDeviceAndRange(deviceId: String, start: Long, end: Long): Flow<List<DeviceSignalEntity>>

    @Query("SELECT * FROM device_signals WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun observeByRange(start: Long, end: Long): Flow<List<DeviceSignalEntity>>

    @Query("DELETE FROM device_signals WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM device_signals")
    suspend fun deleteAll()
}
