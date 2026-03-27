package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transaction_date"]),
        Index(value = ["category"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    @ColumnInfo(name = "transaction_type") val transactionType: String,
    val merchant: String?,
    val category: String?,
    @ColumnInfo(name = "sub_category") val subCategory: String?,
    @ColumnInfo(name = "account_tail") val accountTail: String?,
    val bank: String?,
    @ColumnInfo(name = "upi_id") val upiId: String?,
    @ColumnInfo(name = "balance_after") val balanceAfter: Double?,
    @ColumnInfo(name = "transaction_date") val transactionDate: Long,
    @ColumnInfo(name = "raw_sms_id") val rawSmsId: Long?,
    @ColumnInfo(name = "raw_sms_body") val rawSmsBody: String?,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
    @ColumnInfo(name = "parse_confidence") val parseConfidence: Float = 1f,
    @ColumnInfo(name = "user_verified") val userVerified: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "recurring_group_id") val recurringGroupId: String? = null,
)
