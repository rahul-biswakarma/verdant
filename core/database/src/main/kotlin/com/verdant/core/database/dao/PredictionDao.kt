package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.PredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prediction: PredictionEntity)

    @Query("SELECT * FROM predictions WHERE prediction_type = :type ORDER BY generated_at DESC LIMIT 1")
    suspend fun getLatestByType(type: String): PredictionEntity?

    @Query("SELECT * FROM predictions WHERE expires_at > :now ORDER BY generated_at DESC")
    fun observeActive(now: Long): Flow<List<PredictionEntity>>

    @Query("DELETE FROM predictions WHERE expires_at < :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM predictions")
    suspend fun deleteAll()
}
