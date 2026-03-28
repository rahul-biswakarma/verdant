package com.verdant.core.supabase.repository

import com.verdant.core.database.repository.EmotionalContextRepository
import com.verdant.core.model.EmotionalContext
import com.verdant.core.supabase.dto.EmotionalContextDto
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class EmotionalContextSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : EmotionalContextRepository {

    private val table = "emotional_context"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun getLatest(): EmotionalContext? =
        supabase.postgrest[table]
            .select {
                order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<EmotionalContextDto>()
            ?.toDomain()

    override fun observeLatest(): Flow<EmotionalContext?> = callbackFlow {
        send(getLatest())

        val channel = supabase.realtime.channel("emotional-latest")
        channel.postgresChangeFlow<PostgresAction>("public") {
            table = this@EmotionalContextSupabaseRepository.table
        }.collect { send(getLatest()) }
        channel.subscribe()
        awaitClose { supabase.realtime.removeChannel(channel) }
    }

    override fun observeByRange(start: Long, end: Long): Flow<List<EmotionalContext>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    gte("date", start)
                    lte("date", end)
                }
            }
            .decodeList<EmotionalContextDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("emotional-range")
        channel.postgresChangeFlow<PostgresAction>("public") {
            table = this@EmotionalContextSupabaseRepository.table
        }.collect { send(fetch()) }
        channel.subscribe()
        awaitClose { supabase.realtime.removeChannel(channel) }
    }

    override suspend fun insert(context: EmotionalContext) {
        supabase.postgrest[table].insert(context.toDto(userId()))
    }

    override suspend fun update(context: EmotionalContext) {
        supabase.postgrest[table].update(context.toDto(userId())) {
            filter { eq("id", context.id) }
        }
    }

    override suspend fun deleteOlderThan(before: Long) {
        supabase.postgrest[table].delete {
            filter {
                lt("date", before)
                eq("user_id", userId())
            }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
