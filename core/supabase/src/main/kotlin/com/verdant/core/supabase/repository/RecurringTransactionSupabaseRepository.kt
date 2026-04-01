package com.verdant.core.supabase.repository

import com.verdant.core.model.RecurringTransaction
import com.verdant.core.model.repository.RecurringTransactionRepository
import com.verdant.core.supabase.dto.RecurringTransactionDto
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

class RecurringTransactionSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : RecurringTransactionRepository {

    private val table = "recurring_transactions"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeActive(): Flow<List<RecurringTransaction>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select { filter { eq("is_active", true) } }
            .decodeList<RecurringTransactionDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("recurring-tx-active")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@RecurringTransactionSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun getByMerchant(merchant: String): RecurringTransaction? =
        supabase.postgrest[table]
            .select { filter { eq("merchant", merchant); eq("is_active", true) } }
            .decodeSingleOrNull<RecurringTransactionDto>()
            ?.toDomain()

    override suspend fun insert(recurring: RecurringTransaction) {
        supabase.postgrest[table].insert(recurring.toDto(userId()))
    }

    override suspend fun update(recurring: RecurringTransaction) {
        supabase.postgrest[table].update(recurring.toDto(userId())) {
            filter { eq("id", recurring.id) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
