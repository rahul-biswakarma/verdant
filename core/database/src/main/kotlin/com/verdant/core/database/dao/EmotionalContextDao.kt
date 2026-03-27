package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.verdant.core.database.entity.EmotionalContextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionalContextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(context: EmotionalContextEntity)

    @Update
    suspend fun update(context: EmotionalContextEntity)

    @Query("SELECT * FROM emotional_context ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): EmotionalContextEntity?

    @Query("SELECT * FROM emotional_context ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<EmotionalContextEntity?>

    @Query("SELECT * FROM emotional_context WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeByRange(start: Long, end: Long): Flow<List<EmotionalContextEntity>>

    @Query("DELETE FROM emotional_context WHERE date < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM emotional_context")
    suspend fun deleteAll()
}
