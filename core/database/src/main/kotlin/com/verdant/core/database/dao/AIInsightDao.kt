package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.AIInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIInsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: AIInsightEntity)

    @Query("SELECT * FROM ai_insights WHERE dismissed = 0 ORDER BY generated_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AIInsightEntity>>

    @Query("UPDATE ai_insights SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: String)

    @Query("DELETE FROM ai_insights WHERE expires_at < :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM ai_insights")
    suspend fun deleteAll()
}
