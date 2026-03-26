package com.verdant.core.ai

import com.verdant.core.common.AggregatedHabitData
import com.verdant.core.model.Habit
import java.time.LocalDate

/**
 * Thrown by cloud-only [VerdantAI] methods when the feature cannot be completed.
 *
 * Callers should catch this and show an appropriate UI message based on [reason]:
 *  - NO_NETWORK    → "Connect to the internet for AI insights"
 *  - RATE_LIMITED  → "Daily AI limit reached — try again tomorrow"
 *  - AUTH_ERROR    → trigger sign-in flow
 *  - NOT_SUPPORTED → feature not available in this app build
 */
class AIFeatureUnavailableException(
    message: String,
    val reason: Reason,
    cause: Throwable? = null,
) : Exception(message, cause) {

    enum class Reason { NO_NETWORK, RATE_LIMITED, AUTH_ERROR, NOT_SUPPORTED }

    companion object {
        fun noNetwork() = AIFeatureUnavailableException(
            "Connect to the internet for AI insights.",
            Reason.NO_NETWORK,
        )
        fun rateLimited() = AIFeatureUnavailableException(
            "Daily AI request limit reached. Try again tomorrow.",
            Reason.RATE_LIMITED,
        )
        fun authError(cause: Throwable? = null) = AIFeatureUnavailableException(
            "Please sign in to use AI insights.",
            Reason.AUTH_ERROR,
            cause,
        )
    }
}

data class WeeklyReportData(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    /** Pre-aggregated stats produced by HabitDataAggregator.aggregateForReport(periodDays=7) */
    val aggregatedData: AggregatedHabitData,
    /** Full habit list — used for name resolution when building prompts */
    val habits: List<Habit>,
)

data class WeeklyReport(
    val summary: String,
    val patterns: List<String>,
    val suggestions: List<String>,
    val highlights: List<String>,
)

data class MonthlyReportData(
    val monthStart: LocalDate,
    val monthEnd: LocalDate,
    /** 30-day aggregation */
    val aggregatedData: AggregatedHabitData,
    val habits: List<Habit>,
)

data class MonthlyReport(
    val summary: String,
    val patterns: List<String>,
    val suggestions: List<String>,
    val highlights: List<String>,
)

data class PatternData(
    val habits: List<Habit>,
    val aggregatedData: AggregatedHabitData,
    /** Number of days of history included in aggregatedData */
    val periodDays: Int,
)

data class Pattern(
    val description: String,
    /** 0–1; higher = more statistically reliable */
    val confidence: Float,
    /** IDs of habits involved in this pattern */
    val relatedHabitIds: List<String>,
)

data class CorrelationData(
    val habits: List<Habit>,
    val aggregatedData: AggregatedHabitData,
    val periodDays: Int,
)

data class Correlation(
    val habit1Id: String,
    val habit2Id: String,
    val habit1Name: String,
    val habit2Name: String,
    /** -1..1; positive = habits completed together, negative = inversely related */
    val strength: Float,
    val description: String,
)

/**
 * Compact habit summary passed as context to the AI coach.
 *
 * Keeps the chat payload small by summarising streaks and recent completion rate
 * rather than sending full entry lists.
 */
data class HabitSummary(
    val habits: List<Habit>,
    val recentCompletionRate: Float,
    val activeStreaks: Map<String, Int>,
    val periodDays: Int = 7,
)

// ── Finance AI data classes ──────────────────────────────────────────────────

data class FinanceHistory(
    /** Monthly totals for the last 3–6 months: (yearMonth string → totalSpent) */
    val monthlyTotals: Map<String, Double>,
    /** Category trends: (categoryName → list of monthly amounts, most recent last) */
    val categoryTrends: Map<String, List<Double>>,
    /** Current month's total so far */
    val currentMonthSpent: Double,
    /** Number of months of history available */
    val monthsOfData: Int,
)

data class SpendingSummaryData(
    val totalSpent: Double,
    val totalIncome: Double,
    val categoryBreakdown: Map<String, Double>,
    val topMerchants: List<String>,
    val monthOverMonthChange: Double?,
    val periodDays: Int = 30,
)

/**
 * Cross-product context for the unified dashboard insight.
 * Combines habit performance with financial summary.
 */
data class DashboardContext(
    val habitSummary: HabitSummary?,
    val financeSummary: SpendingSummaryData?,
)
