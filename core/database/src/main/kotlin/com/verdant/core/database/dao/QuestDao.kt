package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.verdant.core.database.entity.QuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quest: QuestEntity)

    @Update
    suspend fun update(quest: QuestEntity)

    @Query("SELECT * FROM quests WHERE status IN ('AVAILABLE', 'ACTIVE') ORDER BY difficulty ASC")
    fun observeActive(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE status = 'COMPLETED' ORDER BY completed_at DESC")
    fun observeCompleted(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun getById(id: String): QuestEntity?

    @Query("UPDATE quests SET status = :status, started_at = :startedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, startedAt: Long?)

    @Query("UPDATE quests SET status = 'COMPLETED', completed_at = :completedAt WHERE id = :id")
    suspend fun complete(id: String, completedAt: Long)

    @Query("DELETE FROM quests WHERE status = 'EXPIRED'")
    suspend fun deleteExpired()

    @Query("DELETE FROM quests")
    suspend fun deleteAll()
}
