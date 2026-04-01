package com.verdant.core.supabase.repository

import com.verdant.core.model.WeatherSnapshot
import com.verdant.core.model.repository.WeatherRepository
import com.verdant.core.supabase.dto.WeatherSnapshotDto
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

class WeatherSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : WeatherRepository {

    private val table = "weather_snapshots"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun insert(snapshot: WeatherSnapshot) {
        supabase.postgrest[table].insert(snapshot.toDto(userId()))
    }

    override fun observeByRange(start: Long, end: Long): Flow<List<WeatherSnapshot>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    gte("date", start)
                    lte("date", end)
                }
                order("date", Order.DESCENDING)
            }
            .decodeList<WeatherSnapshotDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("weather-snapshots-range")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@WeatherSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun getLatest(): WeatherSnapshot? =
        supabase.postgrest[table]
            .select {
                order("date", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<WeatherSnapshotDto>()
            ?.toDomain()

    override suspend fun deleteOlderThan(before: Long) {
        supabase.postgrest[table].delete {
            filter { lt("date", before) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
