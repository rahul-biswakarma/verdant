package com.verdant.core.model.repository

import com.verdant.core.model.Story
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun observeAll(): Flow<List<Story>>
    fun observeRecent(limit: Int): Flow<List<Story>>
    suspend fun getById(id: String): Story?
    suspend fun insert(story: Story)
    suspend fun update(story: Story)
    suspend fun delete(id: String)
    suspend fun getStoriesInRange(start: Long, end: Long): List<Story>
    suspend fun deleteAll()
}
