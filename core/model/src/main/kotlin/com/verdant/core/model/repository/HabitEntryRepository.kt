package com.verdant.core.model.repository

import com.verdant.core.model.HabitEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitEntryRepository {
    fun observeEntries(habitId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<HabitEntry>>
    fun observeAllEntries(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitEntry>>
    suspend fun upsert(entry: HabitEntry)
    suspend fun getByHabitAndDate(habitId: String, date: LocalDate): HabitEntry?
    suspend fun getCompletedDates(habitId: String): List<LocalDate>
    suspend fun delete(entry: HabitEntry)
    suspend fun getById(id: String): HabitEntry?
    suspend fun getAllEntries(): List<HabitEntry>
    suspend fun deleteAllEntries()
}
