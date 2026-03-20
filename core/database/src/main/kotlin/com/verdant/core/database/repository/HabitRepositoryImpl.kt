package com.verdant.core.database.repository

import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.dao.LabelDao
import com.verdant.core.database.entity.toDomain
import com.verdant.core.database.entity.toEntity
import com.verdant.core.model.Habit
import com.verdant.core.model.Label
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
) : HabitRepository {

    override fun observeActiveHabits(): Flow<List<Habit>> =
        habitDao.observeActiveHabits().map { entities -> entities.map { it.toDomain() } }

    override fun observeByLabel(label: String): Flow<List<Habit>> =
        habitDao.getByLabel(label).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): Habit? =
        habitDao.getById(id)?.toDomain()

    override suspend fun insert(habit: Habit) =
        habitDao.insert(habit.toEntity())

    override suspend fun update(habit: Habit) =
        habitDao.update(habit.toEntity())

    override suspend fun delete(habit: Habit) =
        habitDao.delete(habit.toEntity())

    override suspend fun archive(id: String) =
        habitDao.archive(id)

    override suspend fun unarchive(id: String) =
        habitDao.unarchive(id)

    override suspend fun getAllHabits(): List<Habit> =
        habitDao.getAll().map { it.toDomain() }

    override suspend fun deleteAllHabits() =
        habitDao.deleteAll()
}

class LabelRepositoryImpl @Inject constructor(
    private val labelDao: LabelDao,
) : LabelRepository {

    override fun observeAll(): Flow<List<Label>> =
        labelDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): Label? =
        labelDao.getById(id)?.toDomain()

    override suspend fun getAllLabels(): List<Label> =
        labelDao.getAll().map { it.toDomain() }

    override suspend fun insert(label: Label) =
        labelDao.insert(label.toEntity())

    override suspend fun update(label: Label) =
        labelDao.update(label.toEntity())

    override suspend fun delete(label: Label) =
        labelDao.delete(label.toEntity())

    override suspend fun deleteAllLabels() =
        labelDao.deleteAll()
}
