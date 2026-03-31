package com.verdant.core.common.usecase

import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.model.repository.LabelRepository
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
) {
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        habitRepository.deleteAllHabits()   // cascades to habit_entries
        labelRepository.deleteAllLabels()
    }
}
