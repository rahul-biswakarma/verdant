package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.HabitEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: HabitEntryEntity)

    @Query("SELECT * FROM habit_entries WHERE habit_id = :habitId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun observeEntries(habitId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<HabitEntryEntity>>

    @Query("SELECT * FROM habit_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun observeAllEntries(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitEntryEntity>>

    /** Returns epoch days for all completed entries, used for streak calculation. */
    @Query("SELECT date FROM habit_entries WHERE habit_id = :habitId AND completed = 1")
    suspend fun getCompletedDates(habitId: String): List<Long>

    @Delete
    suspend fun delete(entry: HabitEntryEntity)

    @Query("SELECT * FROM habit_entries WHERE id = :id")
    suspend fun getById(id: String): HabitEntryEntity?

    @Query("SELECT * FROM habit_entries WHERE habit_id = :habitId AND date = :date LIMIT 1")
    suspend fun getByHabitAndDate(habitId: String, date: LocalDate): HabitEntryEntity?

    @Query("SELECT * FROM habit_entries ORDER BY date ASC")
    suspend fun getAll(): List<HabitEntryEntity>

    @Query("DELETE FROM habit_entries")
    suspend fun deleteAll()
}
