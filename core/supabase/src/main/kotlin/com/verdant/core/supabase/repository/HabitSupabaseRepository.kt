package com.verdant.core.supabase.repository

import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.model.Habit
import com.verdant.core.supabase.dto.HabitDto
import com.verdant.core.supabase.dto.toDomain
import com.verdant.core.supabase.dto.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class HabitSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : HabitRepository {

    private val table = "habits"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeActiveHabits(): Flow<List<Habit>> = callbackFlow {
        send(fetchActiveHabits())

        try {
            val channel = supabase.realtime.channel("habits-active")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@HabitSupabaseRepository.table
                }.collect {
                    send(fetchActiveHabits())
                }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override fun observeByLabel(label: String): Flow<List<Habit>> = callbackFlow {
        val initial = supabase.postgrest[table]
            .select {
                filter {
                    eq("is_archived", false)
                    eq("label", label)
                }
            }
            .decodeList<HabitDto>()
            .map { it.toDomain() }
        send(initial)

        try {
            val channel = supabase.realtime.channel("habits-label-$label")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@HabitSupabaseRepository.table
                }.collect {
                    val updated = supabase.postgrest[table]
                        .select {
                            filter {
                                eq("is_archived", false)
                                eq("label", label)
                            }
                        }
                        .decodeList<HabitDto>()
                        .map { it.toDomain() }
                    send(updated)
                }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun getById(id: String): Habit? =
        supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<HabitDto>()
            ?.toDomain()

    override suspend fun getAllHabits(): List<Habit> =
        supabase.postgrest[table]
            .select()
            .decodeList<HabitDto>()
            .map { it.toDomain() }

    override suspend fun insert(habit: Habit) {
        supabase.postgrest[table].insert(habit.toDto(userId()))
    }

    override suspend fun update(habit: Habit) {
        supabase.postgrest[table].update(habit.toDto(userId())) {
            filter { eq("id", habit.id) }
        }
    }

    override suspend fun delete(habit: Habit) {
        supabase.postgrest[table].delete {
            filter { eq("id", habit.id) }
        }
    }

    override suspend fun archive(id: String) {
        supabase.postgrest[table].update(mapOf("is_archived" to true)) {
            filter { eq("id", id) }
        }
    }

    override suspend fun unarchive(id: String) {
        supabase.postgrest[table].update(mapOf("is_archived" to false)) {
            filter { eq("id", id) }
        }
    }

    override suspend fun updateTarget(id: String, newTarget: Double) {
        supabase.postgrest[table].update(mapOf("target_value" to newTarget)) {
            filter { eq("id", id) }
        }
    }

    override suspend fun deleteAllHabits() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }

    private suspend fun fetchActiveHabits(): List<Habit> =
        supabase.postgrest[table]
            .select { filter { eq("is_archived", false) } }
            .decodeList<HabitDto>()
            .map { it.toDomain() }
}
