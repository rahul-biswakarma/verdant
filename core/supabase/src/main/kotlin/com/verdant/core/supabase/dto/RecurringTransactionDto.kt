package com.verdant.core.supabase.dto

import com.verdant.core.model.RecurringTransaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecurringTransactionDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val merchant: String,
    val category: String,
    @SerialName("typical_amount") val typicalAmount: Double,
    @SerialName("frequency_days") val frequencyDays: Int,
    @SerialName("last_seen") val lastSeen: Long,
    @SerialName("next_expected") val nextExpected: Long,
    val confidence: Float,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: Long,
)

fun RecurringTransactionDto.toDomain(): RecurringTransaction = RecurringTransaction(
    id = id,
    merchant = merchant,
    category = category,
    typicalAmount = typicalAmount,
    frequencyDays = frequencyDays,
    lastSeen = lastSeen,
    nextExpected = nextExpected,
    confidence = confidence,
    isActive = isActive,
    createdAt = createdAt,
)

fun RecurringTransaction.toDto(userId: String): RecurringTransactionDto = RecurringTransactionDto(
    id = id,
    userId = userId,
    merchant = merchant,
    category = category,
    typicalAmount = typicalAmount,
    frequencyDays = frequencyDays,
    lastSeen = lastSeen,
    nextExpected = nextExpected,
    confidence = confidence,
    isActive = isActive,
    createdAt = createdAt,
)
