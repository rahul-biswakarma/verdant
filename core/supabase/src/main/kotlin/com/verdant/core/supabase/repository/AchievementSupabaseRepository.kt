package com.verdant.core.supabase.repository

import com.verdant.core.model.Achievement
import com.verdant.core.model.repository.AchievementRepository
import com.verdant.core.supabase.dto.AchievementDto
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

class AchievementSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : AchievementRepository {

    private val table = "achievements"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun insert(achievement: Achievement) {
        supabase.postgrest[table].insert(achievement.toDto(userId()))
    }

    override fun observeAll(): Flow<List<Achievement>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select { order("unlocked_at", Order.DESCENDING) }
            .decodeList<AchievementDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("achievements-all")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@AchievementSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun getById(id: String): Achievement? =
        supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<AchievementDto>()
            ?.toDomain()

    override suspend fun count(): Int =
        supabase.postgrest[table]
            .select()
            .decodeList<AchievementDto>()
            .size

    override suspend fun totalXPFromAchievements(): Long? {
        val achievements = supabase.postgrest[table]
            .select()
            .decodeList<AchievementDto>()
        return achievements.sumOf { it.xpReward.toLong() }.takeIf { it > 0 }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
