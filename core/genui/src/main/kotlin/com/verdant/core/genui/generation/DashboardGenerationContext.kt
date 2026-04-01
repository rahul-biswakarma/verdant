package com.verdant.core.genui.generation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Context sent to the Supabase Edge Function (and ultimately to Claude)
 * so it can decide which components to render and in what order.
 */
@Serializable
data class DashboardGenerationContext(
    @SerialName("habit_data") val habitData: HabitDataSummary,
    @SerialName("finance_summary") val financeSummary: FinanceSummary,
    @SerialName("health_summary") val healthSummary: HealthSummary,
    @SerialName("emotional_summary") val emotionalSummary: EmotionalSummary,
    @SerialName("player_summary") val playerSummary: PlayerSummary?,
    @SerialName("day_of_week") val dayOfWeek: String,
    @SerialName("time_of_day") val timeOfDay: String,
)

@Serializable
data class HabitDataSummary(
    @SerialName("total_habits") val totalHabits: Int,
    @SerialName("completion_rate_today") val completionRateToday: Float,
    @SerialName("completion_rate_week") val completionRateWeek: Float,
    @SerialName("best_streak") val bestStreak: Int,
    @SerialName("active_streaks") val activeStreaks: Int,
    @SerialName("top_habits") val topHabits: List<HabitSummaryItem>,
)

@Serializable
data class HabitSummaryItem(
    val name: String,
    @SerialName("completion_rate") val completionRate: Float,
    @SerialName("current_streak") val currentStreak: Int,
)

@Serializable
data class FinanceSummary(
    @SerialName("monthly_spent") val monthlySpent: Double,
    @SerialName("monthly_income") val monthlyIncome: Double,
    @SerialName("has_transactions") val hasTransactions: Boolean,
)

@Serializable
data class HealthSummary(
    @SerialName("avg_steps") val avgSteps: Double,
    @SerialName("avg_sleep_hours") val avgSleepHours: Double,
    @SerialName("has_health_data") val hasHealthData: Boolean,
)

@Serializable
data class EmotionalSummary(
    @SerialName("latest_mood") val latestMood: String?,
    @SerialName("energy_level") val energyLevel: Int?,
)

@Serializable
data class PlayerSummary(
    val level: Int,
    @SerialName("total_xp") val totalXP: Long,
    val rank: String,
)
