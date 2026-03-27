package com.verdant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdant.core.database.entity.PendingAIRequestEntity

@Dao
interface PendingAIRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: PendingAIRequestEntity)

    @Query("SELECT * FROM pending_ai_requests ORDER BY created_at ASC")
    suspend fun getAll(): List<PendingAIRequestEntity>

    @Query("UPDATE pending_ai_requests SET attempt_count = attempt_count + 1 WHERE id = :id")
    suspend fun incrementAttempt(id: String)

    @Query("DELETE FROM pending_ai_requests WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_ai_requests WHERE attempt_count >= :maxAttempts")
    suspend fun deleteFailedRequests(maxAttempts: Int)

    @Query("DELETE FROM pending_ai_requests")
    suspend fun deleteAll()
}
