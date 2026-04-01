package com.verdant.core.supabase.repository

import com.verdant.core.model.LifeScore
import com.verdant.core.model.ScoreType
import com.verdant.core.model.repository.LifeScoreRepository
import com.verdant.core.supabase.dto.LifeScoreDto
import com.verdant.core.supabase.dto.toDomain
import com.verdant.core.supabase.dto.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class LifeScoreSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : LifeScoreRepository {

    private val table = "life_scores"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun getLatestByType(type: ScoreType): LifeScore? =
        supabase.postgrest[table]
            .select {
                filter { eq("score_type", type.name) }
                order("computed_date", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<LifeScoreDto>()
            ?.toDomain()

    override fun observeByRange(start: Long, end: Long): Flow<List<LifeScore>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    gte("computed_date", start)
                    lte("computed_date", end)
                }
            }
            .decodeList<LifeScoreDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("life-scores-range")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@LifeScoreSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override fun observeTrendByType(type: ScoreType, start: Long, end: Long): Flow<List<LifeScore>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    eq("score_type", type.name)
                    gte("computed_date", start)
                    lte("computed_date", end)
                }
                order("computed_date", Order.ASCENDING)
            }
            .decodeList<LifeScoreDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("life-scores-trend")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@LifeScoreSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun insert(score: LifeScore) {
        supabase.postgrest[table].insert(score.toDto(userId()))
    }

    override suspend fun insertAll(scores: List<LifeScore>) {
        val uid = userId()
        supabase.postgrest[table].insert(scores.map { it.toDto(uid) })
    }

    override suspend fun deleteOlderThan(before: Long) {
        supabase.postgrest[table].delete {
            filter { lt("computed_date", before) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
