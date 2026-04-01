package com.verdant.core.supabase.repository

import com.verdant.core.model.Prediction
import com.verdant.core.model.PredictionType
import com.verdant.core.model.repository.PredictionRepository
import com.verdant.core.supabase.dto.PredictionDto
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

class PredictionSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : PredictionRepository {

    private val table = "predictions"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun getLatestByType(type: PredictionType): Prediction? =
        supabase.postgrest[table]
            .select {
                filter { eq("prediction_type", type.name) }
                order("generated_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<PredictionDto>()
            ?.toDomain()

    override fun observeActive(now: Long): Flow<List<Prediction>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select { filter { gt("expires_at", now) } }
            .decodeList<PredictionDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("predictions-active")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@PredictionSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun insert(prediction: Prediction) {
        supabase.postgrest[table].insert(prediction.toDto(userId()))
    }

    override suspend fun deleteExpired(now: Long) {
        supabase.postgrest[table].delete {
            filter { lt("expires_at", now) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
