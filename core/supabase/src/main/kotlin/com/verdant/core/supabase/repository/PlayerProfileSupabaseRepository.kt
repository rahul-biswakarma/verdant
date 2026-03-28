package com.verdant.core.supabase.repository

import com.verdant.core.model.PlayerProfile
import com.verdant.core.model.repository.PlayerProfileRepository
import com.verdant.core.supabase.dto.PlayerProfileDto
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

class PlayerProfileSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : PlayerProfileRepository {

    private val table = "player_profiles"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun get(): PlayerProfile? =
        supabase.postgrest[table]
            .select { filter { eq("user_id", userId()) } }
            .decodeSingleOrNull<PlayerProfileDto>()
            ?.toDomain()

    override fun observe(): Flow<PlayerProfile?> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select { filter { eq("user_id", userId()) } }
            .decodeSingleOrNull<PlayerProfileDto>()
            ?.toDomain()

        send(fetch())

        val channel = supabase.realtime.channel("player-profile")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@PlayerProfileSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override suspend fun insert(profile: PlayerProfile) {
        supabase.postgrest[table].insert(profile.toDto(userId()))
    }

    override suspend fun update(profile: PlayerProfile) {
        supabase.postgrest[table].update(profile.toDto(userId())) {
            filter { eq("id", profile.id) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
