package com.verdant.core.supabase.repository

import com.verdant.core.model.HealthRecord
import com.verdant.core.model.HealthRecordType
import com.verdant.core.model.repository.HealthRecordRepository
import com.verdant.core.supabase.dto.HealthRecordDto
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

class HealthRecordSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : HealthRecordRepository {

    private val table = "health_records"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeByTypeAndRange(type: HealthRecordType, start: Long, end: Long): Flow<List<HealthRecord>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    eq("record_type", type.name)
                    gte("recorded_at", start)
                    lte("recorded_at", end)
                }
                order("recorded_at", Order.DESCENDING)
            }
            .decodeList<HealthRecordDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("health-records-type-range")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@HealthRecordSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override fun observeByRange(start: Long, end: Long): Flow<List<HealthRecord>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    gte("recorded_at", start)
                    lte("recorded_at", end)
                }
                order("recorded_at", Order.DESCENDING)
            }
            .decodeList<HealthRecordDto>()
            .map { it.toDomain() }

        send(fetch())

        try {
            val channel = supabase.realtime.channel("health-records-range")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@HealthRecordSupabaseRepository.table
                }.collect { send(fetch()) }
            }
        } catch (_: Exception) {
            // Realtime unavailable — initial fetch is sufficient
        }
        awaitClose { }
    }

    override suspend fun getLatestByType(type: HealthRecordType): HealthRecord? =
        supabase.postgrest[table]
            .select {
                filter { eq("record_type", type.name) }
                order("recorded_at", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<HealthRecordDto>()
            ?.toDomain()

    override suspend fun insert(record: HealthRecord) {
        supabase.postgrest[table].insert(record.toDto(userId()))
    }

    override suspend fun insertAll(records: List<HealthRecord>) {
        val uid = userId()
        supabase.postgrest[table].insert(records.map { it.toDto(uid) })
    }

    override suspend fun deleteOlderThan(before: Long) {
        supabase.postgrest[table].delete {
            filter { lt("recorded_at", before) }
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
            .decodeList<HealthRecordDto>()
            .size
}
