package com.verdant.core.database.repository

import com.verdant.core.model.Habit
import com.verdant.core.model.Label
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeActiveHabits(): Flow<List<Habit>>
    fun observeByLabel(label: String): Flow<List<Habit>>
    suspend fun getById(id: String): Habit?
    suspend fun getAllHabits(): List<Habit>
    suspend fun insert(habit: Habit)
    suspend fun update(habit: Habit)
    suspend fun delete(habit: Habit)
    suspend fun archive(id: String)
    suspend fun unarchive(id: String)
    suspend fun updateTarget(id: String, newTarget: Double)
    suspend fun deleteAllHabits()
}

interface LabelRepository {
    fun observeAll(): Flow<List<Label>>
    suspend fun getById(id: String): Label?
    suspend fun getAllLabels(): List<Label>
    suspend fun insert(label: Label)
    suspend fun update(label: Label)
    suspend fun delete(label: Label)
    suspend fun deleteAllLabels()
}
