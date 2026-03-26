package com.verdant.core.model

data class Transaction(
    val id: String,
    val amount: Double,
    val type: TransactionType,
    val merchant: String?,
    val category: String?,
    val subCategory: String?,
    val accountTail: String?,
    val bank: String?,
    val upiId: String?,
    val balanceAfter: Double?,
    val transactionDate: Long,
    val rawSmsId: Long?,
    val rawSmsBody: String?,
    val isRecurring: Boolean = false,
    val parseConfidence: Float = 1f,
    val userVerified: Boolean = false,
    val createdAt: Long,
)

enum class TransactionType { DEBIT, CREDIT }
