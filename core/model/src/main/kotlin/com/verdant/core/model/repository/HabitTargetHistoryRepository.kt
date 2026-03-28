package com.verdant.core.model.repository

import com.verdant.core.model.HabitTargetHistory

interface HabitTargetHistoryRepository {
    suspend fun insert(entry: HabitTargetHistory)
    suspend fun getByHabitId(habitId: String): List<HabitTargetHistory>
    suspend fun getAll(): List<HabitTargetHistory>
}
