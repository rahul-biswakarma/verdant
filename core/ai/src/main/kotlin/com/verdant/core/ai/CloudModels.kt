package com.verdant.core.ai

import com.verdant.core.common.AggregatedHabitData
import com.verdant.core.model.Habit
import java.time.LocalDate

// ── Exception ─────────────────────────────────────────────────────────────────

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

// ── Weekly report ─────────────────────────────────────────────────────────────

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

// ── Monthly report ────────────────────────────────────────────────────────────

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

// ── Pattern analysis ──────────────────────────────────────────────────────────

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

// ── Correlation analysis ──────────────────────────────────────────────────────

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

// ── Coach chat ────────────────────────────────────────────────────────────────

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

// ── Fogg Habit Stacking ───────────────────────────────────────────────────────

/**
 * Context for generating a Fogg habit-stack formula.
 *
 * The stack pairs a high-consistency **anchor** habit (≥ 90% completion) with a
 * new or struggling **target** habit. Claude produces a formula like:
 * "After I [anchor], I will [target] for just 2 minutes."
 *
 * @param anchorHabit              The consistent habit that acts as the behavioural cue.
 * @param anchorCompletionRate     0–1 completion rate for the anchor habit.
 * @param anchorConsistentTime     "HH:mm" if the user reliably completes the anchor at a
 *                                 fixed time, null otherwise.
 * @param targetHabit              The new or struggling habit to attach to the anchor.
 * @param targetCompletionRate     0–1 completion rate for the target habit.
 */
data class HabitStackContext(
    val anchorHabit: Habit,
    val anchorCompletionRate: Float,
    val anchorConsistentTime: String?,
    val targetHabit: Habit,
    val targetCompletionRate: Float,
)

// ── Weekly Behavioral Synthesis ───────────────────────────────────────────────

/**
 * Input for the weekly cross-domain behavioral synthesis insight.
 *
 * Combines the standard [AggregatedHabitData] with optional per-habit contextual
 * signals (stress, energy, missed reasons) from Phase 1 entry annotations.
 * These signals are populated when available and null-safe when absent.
 */
data class BehavioralSynthesisData(
    val habits: List<Habit>,
    val aggregatedData: AggregatedHabitData,
    /** Per-habit contextual signals indexed by habit ID. Populated from Phase 1 data if available. */
    val contextualSignals: Map<String, HabitContextSignals> = emptyMap(),
    val periodDays: Int = 7,
)

/**
 * Contextual signals for a single habit derived from entry-level annotations.
 * All fields are averages across the aggregation period.
 */
data class HabitContextSignals(
    /** Average self-reported stress level (1–10) on days the habit was missed. */
    val avgStressOnMiss: Float? = null,
    /** Average self-reported energy level (1–10) on days the habit was completed. */
    val avgEnergyOnComplete: Float? = null,
    /** The most frequently reported reason for missing this habit. */
    val topMissedReason: String? = null,
)

/** Result of the weekly behavioral synthesis — a single cross-domain insight string. */
data class BehavioralSynthesis(
    /** The insight text (1–2 sentences, MI-toned, specific to the user's data). */
    val insight: String,
    /** IDs of the habits referenced in the insight. */
    val relatedHabitIds: List<String>,
)
