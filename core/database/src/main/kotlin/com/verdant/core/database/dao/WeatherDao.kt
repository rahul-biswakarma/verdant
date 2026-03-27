package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.WeatherSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: WeatherSnapshotEntity)

    @Query("SELECT * FROM weather_snapshots WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun observeByRange(start: Long, end: Long): Flow<List<WeatherSnapshotEntity>>

    @Query("SELECT * FROM weather_snapshots ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): WeatherSnapshotEntity?

    @Query("DELETE FROM weather_snapshots WHERE date < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM weather_snapshots")
    suspend fun deleteAll()
}
