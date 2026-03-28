package com.verdant.core.supabase.repository

import com.verdant.core.model.repository.TransactionRepository
import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import com.verdant.core.supabase.dto.TransactionDto
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
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : TransactionRepository {

    private val table = "transactions"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeAll(): Flow<List<Transaction>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select()
            .decodeList<TransactionDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("transactions-all")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@TransactionSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override fun observeByDateRange(start: Long, end: Long): Flow<List<Transaction>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    gte("transaction_date", start)
                    lte("transaction_date", end)
                }
            }
            .decodeList<TransactionDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("transactions-range")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@TransactionSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override fun totalSpent(start: Long, end: Long): Flow<Double?> =
        observeByDateRange(start, end).map { txns ->
            txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }.takeIf { it > 0 }
        }

    override fun totalIncome(start: Long, end: Long): Flow<Double?> =
        observeByDateRange(start, end).map { txns ->
            txns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }.takeIf { it > 0 }
        }

    override suspend fun getById(id: String): Transaction? =
        supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<TransactionDto>()
            ?.toDomain()

    override suspend fun getByRawSmsId(smsId: Long): Transaction? =
        supabase.postgrest[table]
            .select { filter { eq("raw_sms_id", smsId) } }
            .decodeSingleOrNull<TransactionDto>()
            ?.toDomain()

    override suspend fun insert(transaction: Transaction) {
        supabase.postgrest[table].insert(transaction.toDto(userId()))
    }

    override suspend fun insertAll(transactions: List<Transaction>) {
        val uid = userId()
        supabase.postgrest[table].insert(transactions.map { it.toDto(uid) })
    }

    override suspend fun update(transaction: Transaction) {
        supabase.postgrest[table].update(transaction.toDto(userId())) {
            filter { eq("id", transaction.id) }
        }
    }

    override suspend fun delete(transaction: Transaction) {
        supabase.postgrest[table].delete {
            filter { eq("id", transaction.id) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }

    override suspend fun count(): Int =
        supabase.postgrest[table]
            .select()
            .decodeList<TransactionDto>()
            .size
}
