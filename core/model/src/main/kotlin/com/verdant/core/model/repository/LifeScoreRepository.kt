package com.verdant.core.model.repository

import com.verdant.core.model.LifeScore
import com.verdant.core.model.ScoreType
import kotlinx.coroutines.flow.Flow

interface LifeScoreRepository {
    suspend fun getLatestByType(type: ScoreType): LifeScore?
    fun observeByRange(start: Long, end: Long): Flow<List<LifeScore>>
    fun observeTrendByType(type: ScoreType, start: Long, end: Long): Flow<List<LifeScore>>
    suspend fun insert(score: LifeScore)
    suspend fun insertAll(scores: List<LifeScore>)
    suspend fun deleteOlderThan(before: Long)
    suspend fun deleteAll()
}
