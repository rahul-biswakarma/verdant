package com.verdant.core.database.usecase

import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.repository.LabelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clears all user data from Room tables.
 * Habits cascade-delete their entries via FK, so deleting habits also
 * removes all related [HabitEntry] rows.
 */
@Singleton
class DatabaseCleaner @Inject constructor(
    private val habitRepository: HabitRepository,
    private val labelRepository: LabelRepository,
    private val aiInsightDao: AIInsightDao,
) {
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        habitRepository.deleteAllHabits()   // cascades to habit_entries
        labelRepository.deleteAllLabels()
        aiInsightDao.deleteAll()
    }
}
