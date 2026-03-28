package com.verdant.core.supabase.repository

import com.verdant.core.model.Quest
import com.verdant.core.model.repository.QuestRepository
import com.verdant.core.supabase.dto.QuestDto
import com.verdant.core.supabase.dto.toDomain
import com.verdant.core.supabase.dto.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class QuestSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : QuestRepository {

    private val table = "quests"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeActive(): Flow<List<Quest>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select { filter { isIn("status", listOf("AVAILABLE", "ACTIVE")) } }
            .decodeList<QuestDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("quests-active")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@QuestSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override fun observeCompleted(): Flow<List<Quest>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select { filter { eq("status", "COMPLETED") } }
            .decodeList<QuestDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("quests-completed")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@QuestSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override suspend fun getById(id: String): Quest? =
        supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<QuestDto>()
            ?.toDomain()

    override suspend fun insert(quest: Quest) {
        supabase.postgrest[table].insert(quest.toDto(userId()))
    }

    override suspend fun update(quest: Quest) {
        supabase.postgrest[table].update(quest.toDto(userId())) {
            filter { eq("id", quest.id) }
        }
    }

    override suspend fun start(id: String, startedAt: Long) {
        supabase.postgrest[table].update(mapOf("status" to "ACTIVE", "started_at" to startedAt)) {
            filter { eq("id", id) }
        }
    }

    override suspend fun complete(id: String, completedAt: Long) {
        supabase.postgrest[table].update(mapOf("status" to "COMPLETED", "completed_at" to completedAt)) {
            filter { eq("id", id) }
        }
    }

    override suspend fun deleteExpired() {
        supabase.postgrest[table].delete {
            filter { eq("status", "EXPIRED") }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
