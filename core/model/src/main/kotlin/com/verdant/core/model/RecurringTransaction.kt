package com.verdant.core.model

data class RecurringTransaction(
    val id: String,
    val merchant: String,
    val category: String,
    val typicalAmount: Double,
    val frequencyDays: Int,
    val lastSeen: Long,
    val nextExpected: Long,
    val confidence: Float,
    val isActive: Boolean,
    val createdAt: Long,
)
