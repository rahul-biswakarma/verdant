package com.verdant.core.supabase.dto

import com.verdant.core.model.Budget
import com.verdant.core.model.BudgetPeriod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BudgetDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    val category: String,
    val amount: Double,
    val period: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: Long,
)

fun BudgetDto.toDomain(): Budget = Budget(
    id = id,
    name = name,
    category = category,
    amount = amount,
    period = BudgetPeriod.valueOf(period),
    isActive = isActive,
    createdAt = createdAt,
)

fun Budget.toDto(userId: String): BudgetDto = BudgetDto(
    id = id,
    userId = userId,
    name = name,
    category = category,
    amount = amount,
    period = period.name,
    isActive = isActive,
    createdAt = createdAt,
)
