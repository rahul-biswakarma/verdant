package com.verdant.core.model

data class Budget(
    val id: String,
    val name: String,
    val category: String,
    val amount: Double,
    val period: BudgetPeriod,
    val isActive: Boolean,
    val createdAt: Long,
)

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
}
