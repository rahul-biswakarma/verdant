package com.verdant.core.model.repository

import com.verdant.core.model.PlayerProfile
import kotlinx.coroutines.flow.Flow

interface PlayerProfileRepository {
    suspend fun get(): PlayerProfile?
    fun observe(): Flow<PlayerProfile?>
    suspend fun insert(profile: PlayerProfile)
    suspend fun update(profile: PlayerProfile)
    suspend fun deleteAll()
}
