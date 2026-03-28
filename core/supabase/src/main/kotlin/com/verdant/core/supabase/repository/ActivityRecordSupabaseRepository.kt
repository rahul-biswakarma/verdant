package com.verdant.core.supabase.repository

import com.verdant.core.model.ActivityRecord
import com.verdant.core.model.ActivityType
import com.verdant.core.model.repository.ActivityRecordRepository
import com.verdant.core.supabase.dto.ActivityRecordDto
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

class ActivityRecordSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : ActivityRecordRepository {

    private val table = "activity_records"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override suspend fun insert(record: ActivityRecord) {
        supabase.postgrest[table].insert(record.toDto(userId()))
    }

    override suspend fun insertAll(records: List<ActivityRecord>) {
        val uid = userId()
        supabase.postgrest[table].insert(records.map { it.toDto(uid) })
    }

    override fun observeByRange(start: Long, end: Long): Flow<List<ActivityRecord>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter {
                    gte("recorded_at", start)
                    lte("recorded_at", end)
                }
                order("recorded_at", Order.DESCENDING)
            }
            .decodeList<ActivityRecordDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("activity-records-range")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@ActivityRecordSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override suspend fun getLatestByType(type: ActivityType): ActivityRecord? =
        supabase.postgrest[table]
            .select {
                filter { eq("activity_type", type.name) }
                order("recorded_at", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<ActivityRecordDto>()
            ?.toDomain()

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
}
