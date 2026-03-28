package com.verdant.core.supabase.repository

import com.verdant.core.model.DeviceSignal
import com.verdant.core.model.repository.DeviceSignalRepository
import com.verdant.core.supabase.dto.DeviceSignalDto
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

class DeviceSignalSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : DeviceSignalRepository {

    private val table = "device_signals"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun insert(signal: DeviceSignal) {
        supabase.postgrest[table].insert(signal.toDto(userId()))
    }

    override suspend fun insertAll(signals: List<DeviceSignal>) {
        val uid = userId()
        supabase.postgrest[table].insert(signals.map { it.toDto(uid) })
    }

    override fun observeByDeviceAndRange(deviceId: String, start: Long, end: Long): Flow<List<DeviceSignal>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    eq("device_id", deviceId)
                    gte("timestamp", start)
                    lte("timestamp", end)
                }
                order("timestamp", Order.DESCENDING)
            }
            .decodeList<DeviceSignalDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("device-signals-device-range")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@DeviceSignalSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override fun observeByRange(start: Long, end: Long): Flow<List<DeviceSignal>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    gte("timestamp", start)
                    lte("timestamp", end)
                }
                order("timestamp", Order.DESCENDING)
            }
            .decodeList<DeviceSignalDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("device-signals-range")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@DeviceSignalSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override suspend fun deleteOlderThan(before: Long) {
        supabase.postgrest[table].delete {
            filter { lt("timestamp", before) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
