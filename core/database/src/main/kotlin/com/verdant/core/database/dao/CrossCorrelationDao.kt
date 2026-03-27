package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.CrossCorrelationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrossCorrelationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(correlation: CrossCorrelationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(correlations: List<CrossCorrelationEntity>)

    @Query("SELECT * FROM cross_correlations ORDER BY ABS(correlation_strength) DESC")
    fun observeAll(): Flow<List<CrossCorrelationEntity>>

    @Query("SELECT * FROM cross_correlations WHERE ABS(correlation_strength) >= :minStrength ORDER BY ABS(correlation_strength) DESC")
    fun observeSignificant(minStrength: Float): Flow<List<CrossCorrelationEntity>>

    @Query("DELETE FROM cross_correlations")
    suspend fun deleteAll()
}
