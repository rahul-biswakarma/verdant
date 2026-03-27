package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.verdant.core.database.entity.PlayerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: PlayerProfileEntity)

    @Update
    suspend fun update(profile: PlayerProfileEntity)

    @Query("SELECT * FROM player_profile LIMIT 1")
    suspend fun get(): PlayerProfileEntity?

    @Query("SELECT * FROM player_profile LIMIT 1")
    fun observe(): Flow<PlayerProfileEntity?>

    @Query("DELETE FROM player_profile")
    suspend fun deleteAll()
}
