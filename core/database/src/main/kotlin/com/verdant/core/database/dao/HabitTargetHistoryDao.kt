package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.HabitTargetHistoryEntity

@Dao
interface HabitTargetHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HabitTargetHistoryEntity)

    @Query("SELECT * FROM habit_target_history WHERE habit_id = :habitId ORDER BY changed_at DESC")
    suspend fun getByHabitId(habitId: String): List<HabitTargetHistoryEntity>

    @Query("SELECT * FROM habit_target_history ORDER BY changed_at DESC")
    suspend fun getAll(): List<HabitTargetHistoryEntity>
}
