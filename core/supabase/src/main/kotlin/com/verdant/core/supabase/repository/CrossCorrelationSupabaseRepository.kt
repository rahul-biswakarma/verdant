package com.verdant.core.supabase.repository

import com.verdant.core.model.CrossCorrelation
import com.verdant.core.model.repository.CrossCorrelationRepository
import com.verdant.core.supabase.dto.CrossCorrelationDto
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

class CrossCorrelationSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : CrossCorrelationRepository {

    private val table = "cross_correlations"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun insert(correlation: CrossCorrelation) {
        supabase.postgrest[table].insert(correlation.toDto(userId()))
    }

    override suspend fun insertAll(correlations: List<CrossCorrelation>) {
        val uid = userId()
        supabase.postgrest[table].insert(correlations.map { it.toDto(uid) })
    }

    override fun observeAll(): Flow<List<CrossCorrelation>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select()
            .decodeList<CrossCorrelationDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("cross-correlations-all")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@CrossCorrelationSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override fun observeSignificant(minStrength: Float): Flow<List<CrossCorrelation>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select()
            .decodeList<CrossCorrelationDto>()
            .filter { kotlin.math.abs(it.correlationStrength) >= minStrength }
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("cross-correlations-significant")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@CrossCorrelationSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
