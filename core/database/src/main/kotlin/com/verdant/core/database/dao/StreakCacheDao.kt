package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.StreakCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: StreakCacheEntity)

    @Query("SELECT * FROM streak_cache WHERE habit_id = :habitId")
    suspend fun getByHabitId(habitId: String): StreakCacheEntity?

    @Query("SELECT * FROM streak_cache")
    fun observeAll(): Flow<List<StreakCacheEntity>>

    @Query("SELECT * FROM streak_cache")
    suspend fun getAll(): List<StreakCacheEntity>

    @Query("DELETE FROM streak_cache WHERE habit_id = :habitId")
    suspend fun invalidate(habitId: String)

    @Query("DELETE FROM streak_cache")
    suspend fun deleteAll()
}
