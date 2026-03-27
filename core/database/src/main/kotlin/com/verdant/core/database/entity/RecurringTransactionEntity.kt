package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey val id: String,
    val merchant: String,
    val category: String,
    @ColumnInfo(name = "typical_amount") val typicalAmount: Double,
    @ColumnInfo(name = "frequency_days") val frequencyDays: Int,
    @ColumnInfo(name = "last_seen") val lastSeen: Long,
    @ColumnInfo(name = "next_expected") val nextExpected: Long,
    val confidence: Float,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
