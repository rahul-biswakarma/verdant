package com.verdant.core.supabase.repository

import com.verdant.core.model.Budget
import com.verdant.core.model.repository.BudgetRepository
import com.verdant.core.supabase.dto.BudgetDto
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

class BudgetSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : BudgetRepository {

    private val table = "budgets"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeActive(): Flow<List<Budget>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select { filter { eq("is_active", true) } }
            .decodeList<BudgetDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("budgets-active")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@BudgetSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override suspend fun getByCategory(category: String): Budget? =
        supabase.postgrest[table]
            .select { filter { eq("category", category); eq("is_active", true) } }
            .decodeSingleOrNull<BudgetDto>()
            ?.toDomain()

    override suspend fun getById(id: String): Budget? =
        supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<BudgetDto>()
            ?.toDomain()

    override suspend fun insert(budget: Budget) {
        supabase.postgrest[table].insert(budget.toDto(userId()))
    }

    override suspend fun update(budget: Budget) {
        supabase.postgrest[table].update(budget.toDto(userId())) {
            filter { eq("id", budget.id) }
        }
    }

    override suspend fun delete(budget: Budget) {
        supabase.postgrest[table].delete {
            filter { eq("id", budget.id) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
