package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.HealthRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HealthRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HealthRecordEntity>)

    @Query("SELECT * FROM health_records WHERE record_type = :type AND recorded_at BETWEEN :start AND :end ORDER BY recorded_at DESC")
    fun observeByTypeAndRange(type: String, start: Long, end: Long): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_records WHERE recorded_at BETWEEN :start AND :end ORDER BY recorded_at DESC")
    fun observeByRange(start: Long, end: Long): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_records WHERE record_type = :type ORDER BY recorded_at DESC LIMIT 1")
    suspend fun getLatestByType(type: String): HealthRecordEntity?

    @Query("DELETE FROM health_records WHERE recorded_at < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM health_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM health_records")
    suspend fun count(): Int
}
