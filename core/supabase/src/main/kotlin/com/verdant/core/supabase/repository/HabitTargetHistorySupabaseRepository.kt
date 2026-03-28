package com.verdant.core.supabase.repository

import com.verdant.core.model.HabitTargetHistory
import com.verdant.core.model.repository.HabitTargetHistoryRepository
import com.verdant.core.supabase.dto.HabitTargetHistoryDto
import com.verdant.core.supabase.dto.toDomain
import com.verdant.core.supabase.dto.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class HabitTargetHistorySupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : HabitTargetHistoryRepository {

    private val table = "habit_target_history"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun insert(entry: HabitTargetHistory) {
        supabase.postgrest[table].insert(entry.toDto(userId()))
    }

    override suspend fun getByHabitId(habitId: String): List<HabitTargetHistory> =
        supabase.postgrest[table]
            .select {
                filter { eq("habit_id", habitId) }
                order("changed_at", Order.DESCENDING)
            }
            .decodeList<HabitTargetHistoryDto>()
            .map { it.toDomain() }

    override suspend fun getAll(): List<HabitTargetHistory> =
        supabase.postgrest[table]
            .select { order("changed_at", Order.DESCENDING) }
            .decodeList<HabitTargetHistoryDto>()
            .map { it.toDomain() }
}
