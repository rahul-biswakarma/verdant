package com.verdant.core.supabase.repository

import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.HabitEntry
import com.verdant.core.supabase.dto.HabitEntryDto
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
import java.time.LocalDate
import javax.inject.Inject

class HabitEntrySupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : HabitEntryRepository {

    private val table = "habit_entries"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeEntries(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<HabitEntry>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    eq("habit_id", habitId)
                    gte("date", startDate.toString())
                    lte("date", endDate.toString())
                }
            }
            .decodeList<HabitEntryDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("entries-$habitId")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@HabitEntrySupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override fun observeAllEntries(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<HabitEntry>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    gte("date", startDate.toString())
                    lte("date", endDate.toString())
                }
            }
            .decodeList<HabitEntryDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("entries-all")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@HabitEntrySupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun upsert(entry: HabitEntry) {
        supabase.postgrest[table].upsert(entry.toDto(userId()))
    }

    override suspend fun getByHabitAndDate(habitId: String, date: LocalDate): HabitEntry? =
        supabase.postgrest[table]
            .select {
                filter {
                    eq("habit_id", habitId)
                    eq("date", date.toString())
                }
            }
            .decodeSingleOrNull<HabitEntryDto>()
            ?.toDomain()

    override suspend fun getCompletedDates(habitId: String): List<LocalDate> =
        supabase.postgrest[table]
            .select {
                filter {
                    eq("habit_id", habitId)
                    eq("completed", true)
                }
            }
            .decodeList<HabitEntryDto>()
            .map { LocalDate.parse(it.date) }

    override suspend fun delete(entry: HabitEntry) {
        supabase.postgrest[table].delete {
            filter { eq("id", entry.id) }
        }
    }

    override suspend fun getById(id: String): HabitEntry? =
        supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<HabitEntryDto>()
            ?.toDomain()

    override suspend fun getAllEntries(): List<HabitEntry> =
        supabase.postgrest[table]
            .select()
            .decodeList<HabitEntryDto>()
            .map { it.toDomain() }

    override suspend fun deleteAllEntries() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
