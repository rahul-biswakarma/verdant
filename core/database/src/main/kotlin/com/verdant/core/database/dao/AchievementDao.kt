package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements ORDER BY unlocked_at DESC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getById(id: String): AchievementEntity?

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun count(): Int

    @Query("SELECT SUM(xp_reward) FROM achievements")
    suspend fun totalXPFromAchievements(): Long?

    @Query("DELETE FROM achievements")
    suspend fun deleteAll()
}
