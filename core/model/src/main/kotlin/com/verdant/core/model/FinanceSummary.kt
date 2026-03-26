package com.verdant.core.model

import java.time.YearMonth

data class MonthlySpendingSummary(
    val month: YearMonth,
    val totalSpent: Double,
    val totalIncome: Double,
    val categoryBreakdown: List<CategorySpend>,
    val topMerchants: List<MerchantSpend>,
    val comparedToLastMonth: Double?,
)

data class CategorySpend(
    val category: String,
    val amount: Double,
    val percentage: Float,
)

data class MerchantSpend(
    val merchant: String,
    val amount: Double,
    val transactionCount: Int,
)

data class MonthlyPrediction(
    val predictedTotal: Double,
    val categoryPredictions: Map<String, Double>,
    val unusualItems: List<String>,
    val confidence: Float,
)
