package com.verdant.core.supabase.repository

import com.verdant.core.model.StoryEvent
import com.verdant.core.model.repository.StoryEventRepository
import com.verdant.core.supabase.dto.StoryEventDto
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

class StoryEventSupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : StoryEventRepository {

    private val table = "story_events"
    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("User not authenticated")

    override fun observeByStoryId(storyId: String): Flow<List<StoryEvent>> = callbackFlow {
        suspend fun fetch() = supabase.postgrest[table]
            .select {
                filter { eq("story_id", storyId) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<StoryEventDto>()
            .map { it.toDomain() }

        send(fetch())

        val channel = supabase.realtime.channel("story-events-$storyId")
        channel.subscribe()
        launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                table = this@StoryEventSupabaseRepository.table
            }.collect { send(fetch()) }
        }
        awaitClose { }
    }

    override suspend fun getByStoryId(storyId: String): List<StoryEvent> =
        supabase.postgrest[table]
            .select {
                filter { eq("story_id", storyId) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<StoryEventDto>()
            .map { it.toDomain() }

    override suspend fun insert(event: StoryEvent) {
        supabase.postgrest[table].insert(event.toDto(userId()))
    }

    override suspend fun insertAll(events: List<StoryEvent>) {
        if (events.isEmpty()) return
        val uid = userId()
        supabase.postgrest[table].insert(events.map { it.toDto(uid) })
    }

    override suspend fun delete(id: String) {
        supabase.postgrest[table].delete {
            filter { eq("id", id) }
        }
    }

    override suspend fun deleteByStoryId(storyId: String) {
        supabase.postgrest[table].delete {
            filter { eq("story_id", storyId) }
        }
    }

    override suspend fun deleteAll() {
        supabase.postgrest[table].delete {
            filter { eq("user_id", userId()) }
        }
    }
}
