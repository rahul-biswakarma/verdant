package com.verdant.core.supabase.repository

import com.verdant.core.model.PendingAIRequest
import com.verdant.core.model.repository.PendingAIRequestRepository
import com.verdant.core.supabase.dto.PendingAIRequestDto
import com.verdant.core.supabase.dto.toDomain
import com.verdant.core.supabase.dto.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class PendingAIRequestSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : PendingAIRequestRepository {

    private val table = "pending_ai_requests"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun insert(request: PendingAIRequest) {
        supabase.postgrest[table].insert(request.toDto(userId()))
    }

    override suspend fun getAll(): List<PendingAIRequest> =
        supabase.postgrest[table]
            .select { order("created_at", Order.ASCENDING) }
            .decodeList<PendingAIRequestDto>()
            .map { it.toDomain() }

    override suspend fun incrementAttempt(id: String) {
        val current = supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<PendingAIRequestDto>()
        if (current != null) {
            supabase.postgrest[table].update(mapOf("attempt_count" to current.attemptCount + 1)) {
                filter { eq("id", id) }
            }
        }
    }

    override suspend fun delete(id: String) {
        supabase.postgrest[table].delete {
            filter { eq("id", id) }
        }
    }

    override suspend fun getPending(maxRetries: Int): List<PendingAIRequest> =
        supabase.postgrest[table]
            .select {
                filter { lt("attempt_count", maxRetries) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<PendingAIRequestDto>()
            .map { it.toDomain() }

    override suspend fun deleteFailedRequests(maxAttempts: Int) {
        supabase.postgrest[table].delete {
            filter { gte("attempt_count", maxAttempts) }
        }
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        supabase.postgrest[table].delete {
            filter { lt("created_at", cutoff) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
