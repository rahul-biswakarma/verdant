package com.verdant.core.supabase.repository

import com.verdant.core.model.repository.LabelRepository
import com.verdant.core.model.Label
import com.verdant.core.supabase.dto.LabelDto
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

class LabelSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : LabelRepository {

    private val table = "labels"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeAll(): Flow<List<Label>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select()
            .decodeList<LabelDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("labels-all")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@LabelSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun getById(id: String): Label? =
        supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<LabelDto>()
            ?.toDomain()

    override suspend fun getAllLabels(): List<Label> =
        supabase.postgrest[table]
            .select()
            .decodeList<LabelDto>()
            .map { it.toDomain() }

    override suspend fun insert(label: Label) {
        supabase.postgrest[table].insert(label.toDto(userId()))
    }

    override suspend fun update(label: Label) {
        supabase.postgrest[table].update(label.toDto(userId())) {
            filter { eq("id", label.id) }
        }
    }

    override suspend fun delete(label: Label) {
        supabase.postgrest[table].delete {
            filter { eq("id", label.id) }
        }
    }

    override suspend fun deleteAllLabels() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
