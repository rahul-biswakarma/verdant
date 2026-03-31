package com.verdant.core.common

// ── Prediction context — all-domain aggregate for Claude ─────────────────────

/**
 * Comprehensive cross-domain context sent to Claude for daily predictions.
 * Each field is a compact summary — the full aggregator keeps the total
 * payload under ~2 000 tokens.
 */
data class PredictionContext(
    val habitData: AggregatedHabitData,
    val financeSummary: CompactFinanceSummary,
    val healthSummary: CompactHealthSummary,
    val emotionalSummary: CompactEmotionalSummary,
    val activitySummary: CompactActivitySummary,
    val deviceStats: CompactDeviceStats,
    /** Pre-computed local scores from the statistical prediction models. */
    val statisticalPredictions: StatisticalPredictions,
)

data class CompactFinanceSummary(
    val monthlySpent: Double,
    val monthlyIncome: Double,
    /** Top 3 categories by spend: category name -> amount. */
    val topCategories: List<Pair<String, Double>>,
    /** "increasing", "decreasing", or "stable". */
    val spendingTrend: String,
    val predictedNextMonth: Double?,
)

data class CompactHealthSummary(
    val avgSteps7d: Double,
    val avgSleepHours7d: Double,
    val avgHeartRate7d: Double,
    val exerciseMinutes7d: Double,
    val weightTrend: String,
)

data class CompactEmotionalSummary(
    /** Most frequent mood in the last 7 days. */
    val dominantMood7d: String,
    val avgEnergy7d: Float,
    val avgStress7d: Float,
    val moodDistribution: Map<String, Int>,
)

data class CompactActivitySummary(
    /** Most frequent activity type. */
    val dominantActivity: String,
    val totalActivities7d: Int,
    val activityBreakdown: Map<String, Int>,
)

data class CompactDeviceStats(
    val avgScreenTimeMinutes7d: Double,
    val avgNotifications7d: Double,
)

data class StatisticalPredictions(
    val spendingForecast: Double,
    val spendingConfidence: Float,
    /** Habit ID -> sustainability score (0-1). */
    val habitSustainability: Map<String, Float>,
    val healthTrajectory: Map<String, Double>,
    val stressIndex: Float?,
    val financialHealthScore: Int?,
    val lifestyleScore: Int?,
)

// ── Prediction result — structured output from Claude ────────────────────────

data class PredictionResult(
    val spending: PredictionItem,
    val habits: PredictionItem,
    val health: PredictionItem,
    val life: PredictionItem,
)

data class PredictionItem(
    val summary: String,
    val details: String,
    val confidence: Float,
    val keyInsights: List<String>,
)
