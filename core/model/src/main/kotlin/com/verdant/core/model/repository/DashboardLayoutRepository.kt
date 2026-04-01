package com.verdant.core.model.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persistence for LLM-generated dashboard layouts.
 * Layout JSON is stored as a text blob alongside metadata.
 */
interface DashboardLayoutRepository {
    suspend fun save(layoutJson: String, generatedAt: Long, expiresAt: Long, schemaVersion: Int)
    suspend fun getLatestJson(): String?
    fun observeLatestJson(): Flow<String?>
    suspend fun deleteExpired(now: Long)
    suspend fun deleteAll()
}
