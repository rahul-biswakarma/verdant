package com.verdant.core.model.repository

import com.verdant.core.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun insert(snapshot: WeatherSnapshot)
    fun observeByRange(start: Long, end: Long): Flow<List<WeatherSnapshot>>
    suspend fun getLatest(): WeatherSnapshot?
    suspend fun deleteOlderThan(before: Long)
    suspend fun deleteAll()
}
