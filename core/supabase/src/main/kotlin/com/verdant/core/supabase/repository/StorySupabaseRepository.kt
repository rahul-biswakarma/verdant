package com.verdant.core.supabase.repository

import com.verdant.core.model.Story
import com.verdant.core.model.repository.StoryRepository
import com.verdant.core.supabase.dto.StoryDto
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class StorySupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : StoryRepository {

    private val table = "stories"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeAll(): Flow<List<Story>> = callbackFlow {
        val initial = try {
            supabase.postgrest[table]
                .select { order("start_time", Order.DESCENDING) }
                .decodeList<StoryDto>()
                .map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
        send(initial)

        try {
            val channel = supabase.realtime.channel("stories-all")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@StorySupabaseRepository.table
                }.collect {
                    val updated = try {
                        supabase.postgrest[this@StorySupabaseRepository.table]
                            .select { order("start_time", Order.DESCENDING) }
                            .decodeList<StoryDto>()
                            .map { it.toDomain() }
                    } catch (_: Exception) {
                        emptyList()
                    }
                    send(updated)
                }
            }
        } catch (_: Exception) {
            // Realtime not available — initial fetch is enough
        }
        awaitClose { }
    }

    override fun observeRecent(limit: Int): Flow<List<Story>> = callbackFlow {
        val initial = try {
            supabase.postgrest[table]
                .select {
                    order("start_time", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<StoryDto>()
                .map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
        send(initial)

        try {
            val channel = supabase.realtime.channel("stories-recent")
            channel.subscribe()
            launch {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    table = this@StorySupabaseRepository.table
                }.collect {
                    val updated = try {
                        supabase.postgrest[this@StorySupabaseRepository.table]
                            .select {
                                order("start_time", Order.DESCENDING)
                                limit(limit.toLong())
                            }
                            .decodeList<StoryDto>()
                            .map { it.toDomain() }
                    } catch (_: Exception) {
                        emptyList()
                    }
                    send(updated)
                }
            }
        } catch (_: Exception) { }
        awaitClose { }
    }

    override suspend fun getById(id: String): Story? = try {
        supabase.postgrest[table]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<StoryDto>()
            ?.toDomain()
    } catch (_: Exception) {
        null
    }

    override suspend fun insert(story: Story) {
        supabase.postgrest[table].insert(story.toDto(userId()))
    }

    override suspend fun update(story: Story) {
        supabase.postgrest[table].update(story.toDto(userId())) {
            filter { eq("id", story.id) }
        }
    }

    override suspend fun delete(id: String) {
        supabase.postgrest[table].delete {
            filter { eq("id", id) }
        }
    }

    override suspend fun getStoriesInRange(start: Long, end: Long): List<Story> = try {
        supabase.postgrest[table]
            .select {
                filter {
                    gte("start_time", start)
                    lte("start_time", end)
                }
                order("start_time", Order.DESCENDING)
            }
            .decodeList<StoryDto>()
            .map { it.toDomain() }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
