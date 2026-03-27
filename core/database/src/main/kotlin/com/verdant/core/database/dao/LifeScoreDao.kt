package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.LifeScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: LifeScoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scores: List<LifeScoreEntity>)

    @Query("SELECT * FROM life_scores WHERE score_type = :type ORDER BY computed_date DESC LIMIT 1")
    suspend fun getLatestByType(type: String): LifeScoreEntity?

    @Query("SELECT * FROM life_scores WHERE computed_date BETWEEN :start AND :end ORDER BY computed_date DESC")
    fun observeByRange(start: Long, end: Long): Flow<List<LifeScoreEntity>>

    @Query("SELECT * FROM life_scores WHERE score_type = :type AND computed_date BETWEEN :start AND :end ORDER BY computed_date ASC")
    fun observeTrendByType(type: String, start: Long, end: Long): Flow<List<LifeScoreEntity>>

    @Query("DELETE FROM life_scores WHERE computed_date < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM life_scores")
    suspend fun deleteAll()
}
