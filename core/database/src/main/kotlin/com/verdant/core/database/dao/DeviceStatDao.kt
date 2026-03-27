package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.DeviceStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceStatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stat: DeviceStatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stats: List<DeviceStatEntity>)

    @Query("SELECT * FROM device_stats WHERE stat_type = :type AND recorded_date BETWEEN :start AND :end ORDER BY recorded_date DESC")
    fun observeByTypeAndRange(type: String, start: Long, end: Long): Flow<List<DeviceStatEntity>>

    @Query("SELECT * FROM device_stats WHERE recorded_date BETWEEN :start AND :end ORDER BY recorded_date DESC")
    fun observeByRange(start: Long, end: Long): Flow<List<DeviceStatEntity>>

    @Query("SELECT * FROM device_stats WHERE stat_type = :type ORDER BY recorded_date DESC LIMIT 1")
    suspend fun getLatestByType(type: String): DeviceStatEntity?

    @Query("DELETE FROM device_stats WHERE recorded_date < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM device_stats")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM device_stats")
    suspend fun count(): Int
}
