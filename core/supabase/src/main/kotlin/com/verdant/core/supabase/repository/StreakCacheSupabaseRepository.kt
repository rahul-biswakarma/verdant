package com.verdant.core.supabase.repository

import com.verdant.core.model.StreakCache
import com.verdant.core.model.repository.StreakCacheRepository
import com.verdant.core.supabase.dto.StreakCacheDto
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

class StreakCacheSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : StreakCacheRepository {

    private val table = "streak_cache"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun upsert(cache: StreakCache) {
        supabase.postgrest[table].upsert(cache.toDto(userId()))
    }

    override suspend fun getByHabitId(habitId: String): StreakCache? =
        supabase.postgrest[table]
            .select { filter { eq("habit_id", habitId) } }
            .decodeSingleOrNull<StreakCacheDto>()
            ?.toDomain()

    override fun observeAll(): Flow<List<StreakCache>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select()
            .decodeList<StreakCacheDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("streak-cache-all")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@StreakCacheSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override suspend fun getAll(): List<StreakCache> =
        supabase.postgrest[table]
            .select()
            .decodeList<StreakCacheDto>()
            .map { it.toDomain() }

    override suspend fun invalidate(habitId: String) {
        supabase.postgrest[table].delete {
            filter { eq("habit_id", habitId) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
