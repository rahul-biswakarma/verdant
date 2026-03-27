package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.ActivityRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ActivityRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ActivityRecordEntity>)

    @Query("SELECT * FROM activity_records WHERE recorded_at BETWEEN :start AND :end ORDER BY recorded_at DESC")
    fun observeByRange(start: Long, end: Long): Flow<List<ActivityRecordEntity>>

    @Query("SELECT * FROM activity_records WHERE activity_type = :type ORDER BY recorded_at DESC LIMIT 1")
    suspend fun getLatestByType(type: String): ActivityRecordEntity?

    @Query("DELETE FROM activity_records WHERE recorded_at < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM activity_records")
    suspend fun deleteAll()
}
