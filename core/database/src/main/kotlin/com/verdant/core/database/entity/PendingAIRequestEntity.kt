package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_ai_requests")
data class PendingAIRequestEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "request_type") val requestType: String,
    val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int,
)
