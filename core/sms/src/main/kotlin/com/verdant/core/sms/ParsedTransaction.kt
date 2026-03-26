package com.verdant.core.sms

import com.verdant.core.model.TransactionType

data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val merchant: String?,
    val accountTail: String?,
    val bank: String?,
    val upiId: String?,
    val balance: Double?,
    val date: Long,
    val rawSmsId: Long,
    val rawSmsBody: String,
    val confidence: Float,
)
