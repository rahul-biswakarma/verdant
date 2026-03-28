package com.verdant.core.supabase.repository

import com.verdant.core.model.AIInsight
import com.verdant.core.model.repository.AIInsightRepository
import com.verdant.core.supabase.dto.AIInsightDto
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

class AIInsightSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : AIInsightRepository {

    private val table = "ai_insights"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun insert(insight: AIInsight) {
        supabase.postgrest[table].insert(insight.toDto(userId()))
    }

    override fun observeRecent(limit: Int): Flow<List<AIInsight>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter { eq("dismissed", false) }
                order("generated_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<AIInsightDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("ai-insights-recent")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@AIInsightSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override suspend fun dismiss(id: String) {
        supabase.postgrest[table].update(mapOf("dismissed" to true)) {
            filter { eq("id", id) }
        }
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
