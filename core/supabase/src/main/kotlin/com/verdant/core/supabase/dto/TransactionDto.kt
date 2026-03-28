package com.verdant.core.supabase.dto

import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val amount: Double,
    val type: String,
    val merchant: String? = null,
    val category: String? = null,
    @SerialName("sub_category") val subCategory: String? = null,
    @SerialName("account_tail") val accountTail: String? = null,
    val bank: String? = null,
    @SerialName("upi_id") val upiId: String? = null,
    @SerialName("balance_after") val balanceAfter: Double? = null,
    @SerialName("transaction_date") val transactionDate: Long,
    @SerialName("raw_sms_id") val rawSmsId: Long? = null,
    @SerialName("raw_sms_body") val rawSmsBody: String? = null,
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    @SerialName("parse_confidence") val parseConfidence: Float = 1f,
    @SerialName("user_verified") val userVerified: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
)

fun TransactionDto.toDomain(): Transaction = Transaction(
    id = id,
    amount = amount,
    type = TransactionType.valueOf(type),
    merchant = merchant,
    category = category,
    subCategory = subCategory,
    accountTail = accountTail,
    bank = bank,
    upiId = upiId,
    balanceAfter = balanceAfter,
    transactionDate = transactionDate,
    rawSmsId = rawSmsId,
    rawSmsBody = rawSmsBody,
    isRecurring = isRecurring,
    parseConfidence = parseConfidence,
    userVerified = userVerified,
    createdAt = createdAt,
)

fun Transaction.toDto(userId: String): TransactionDto = TransactionDto(
    id = id,
    userId = userId,
    amount = amount,
    type = type.name,
    merchant = merchant,
    category = category,
    subCategory = subCategory,
    accountTail = accountTail,
    bank = bank,
    upiId = upiId,
    balanceAfter = balanceAfter,
    transactionDate = transactionDate,
    rawSmsId = rawSmsId,
    rawSmsBody = rawSmsBody,
    isRecurring = isRecurring,
    parseConfidence = parseConfidence,
    userVerified = userVerified,
    createdAt = createdAt,
)
