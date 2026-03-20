package com.verdant.core.database.repository

import com.verdant.core.database.dao.HabitEntryDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.HabitEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

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

class HabitEntryRepositoryImpl @Inject constructor(
    private val dao: HabitEntryDao,
) : HabitEntryRepository {

    override fun observeEntries(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<HabitEntry>> =
        dao.observeEntries(habitId, startDate, endDate).map { it.map { e -> e.toDomain() } }

    override fun observeAllEntries(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<HabitEntry>> =
        dao.observeAllEntries(startDate, endDate).map { it.map { e -> e.toDomain() } }

    override suspend fun upsert(entry: HabitEntry) =
        dao.upsert(entry.toEntity())

    override suspend fun getByHabitAndDate(habitId: String, date: LocalDate): HabitEntry? =
        dao.getByHabitAndDate(habitId, date)?.toDomain()

    override suspend fun getCompletedDates(habitId: String): List<LocalDate> =
        dao.getCompletedDates(habitId).map { LocalDate.ofEpochDay(it) }

    override suspend fun delete(entry: HabitEntry) =
        dao.delete(entry.toEntity())

    override suspend fun getById(id: String): HabitEntry? =
        dao.getById(id)?.toDomain()

    override suspend fun getAllEntries(): List<HabitEntry> =
        dao.getAll().map { it.toDomain() }

    override suspend fun deleteAllEntries() =
        dao.deleteAll()
}
