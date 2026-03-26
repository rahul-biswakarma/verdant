package com.verdant.core.model

data class HabitsDashboardCard(
    val completedCount: Int,
    val totalCount: Int,
    val topStreak: Pair<String, Int>?,
    val atRiskHabits: List<String>,
)

data class FinanceDashboardCard(
    val monthlySpent: Double,
    val topCategory: Pair<String, Double>?,
    val lastTransactionAge: String?,
)

data class DashboardAlert(
    val product: VerdantProduct,
    val type: AlertType,
    val title: String,
    val description: String,
)

enum class AlertType { AT_RISK, UNUSUAL, MILESTONE }

data class DashboardHighlight(
    val product: VerdantProduct,
    val text: String,
)
