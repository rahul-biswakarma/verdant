package com.verdant.core.supabase.repository

import com.verdant.core.model.DeviceStat
import com.verdant.core.model.DeviceStatType
import com.verdant.core.model.repository.DeviceStatRepository
import com.verdant.core.supabase.dto.DeviceStatDto
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

class DeviceStatSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : DeviceStatRepository {

    private val table = "device_stats"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeByTypeAndRange(type: DeviceStatType, start: Long, end: Long): Flow<List<DeviceStat>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    eq("stat_type", type.name)
                    gte("recorded_date", start)
                    lte("recorded_date", end)
                }
                order("recorded_date", Order.DESCENDING)
            }
            .decodeList<DeviceStatDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("device-stats-type-range")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@DeviceStatSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override fun observeByRange(start: Long, end: Long): Flow<List<DeviceStat>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    gte("recorded_date", start)
                    lte("recorded_date", end)
                }
                order("recorded_date", Order.DESCENDING)
            }
            .decodeList<DeviceStatDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("device-stats-range")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@DeviceStatSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun getLatestByType(type: DeviceStatType): DeviceStat? =
        supabase.postgrest[table]
            .select {
                filter { eq("stat_type", type.name) }
                order("recorded_date", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<DeviceStatDto>()
            ?.toDomain()

    override suspend fun insert(stat: DeviceStat) {
        supabase.postgrest[table].insert(stat.toDto(userId()))
    }

    override suspend fun insertAll(stats: List<DeviceStat>) {
        val uid = userId()
        supabase.postgrest[table].insert(stats.map { it.toDto(uid) })
    }

    override suspend fun deleteOlderThan(before: Long) {
        supabase.postgrest[table].delete {
            filter { lt("recorded_date", before) }
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
            .decodeList<DeviceStatDto>()
            .size
}
