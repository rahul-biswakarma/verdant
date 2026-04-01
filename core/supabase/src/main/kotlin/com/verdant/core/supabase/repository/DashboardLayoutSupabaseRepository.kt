package com.verdant.core.supabase.repository

import com.verdant.core.model.repository.DashboardLayoutRepository
import com.verdant.core.supabase.dto.DashboardLayoutDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

class DashboardLayoutSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : DashboardLayoutRepository {

    private val table = "dashboard_layouts"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun save(
        layoutJson: String,
        generatedAt: Long,
        expiresAt: Long,
        schemaVersion: Int,
    ) {
        val dto = DashboardLayoutDto(
            id = UUID.randomUUID().toString(),
            userId = userId(),
            layoutJson = layoutJson,
            generatedAt = generatedAt,
            expiresAt = expiresAt,
            schemaVersion = schemaVersion,
        )
        supabase.postgrest[table].insert(dto)
    }

    override suspend fun getLatestJson(): String? =
        supabase.postgrest[table]
            .select {
                filter { eq("user_id", userId()) }
                order("generated_at", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<DashboardLayoutDto>()
            ?.layoutJson

    override fun observeLatestJson(): Flow<String?> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter { eq("user_id", userId()) }
                order("generated_at", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<DashboardLayoutDto>()
            ?.layoutJson

        try { send(fetch()) } catch (_: Exception) { send(null) }

        try {
            val channel = supabase.realtime.channel("dashboard-layout")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@DashboardLayoutSupabaseRepository.table
                }.collect { try { send(fetch()) } catch (_: Exception) { } }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
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
