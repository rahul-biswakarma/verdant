package com.verdant.core.model.repository

import com.verdant.core.model.StoryEvent
import kotlinx.coroutines.flow.Flow

interface StoryEventRepository {
    fun observeByStoryId(storyId: String): Flow<List<StoryEvent>>
    suspend fun getByStoryId(storyId: String): List<StoryEvent>
    suspend fun insert(event: StoryEvent)
    suspend fun insertAll(events: List<StoryEvent>)
    suspend fun delete(id: String)
    suspend fun deleteByStoryId(storyId: String)
    suspend fun deleteAll()
}
