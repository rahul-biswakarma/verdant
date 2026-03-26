package com.verdant.core.ai

import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.model.ChatMessage
import com.verdant.core.model.Habit
import kotlinx.coroutines.flow.Flow

/**
 * Availability status of the on-device AI model (MediaPipe LLM / Gemma 2B).
 *
 *   AVAILABLE     – model is downloaded and ready for inference
 *   DOWNLOADING   – model download is currently in progress
 *   DOWNLOADABLE  – device is supported; model can be downloaded
 *   UNAVAILABLE   – device is not supported (< 6 GB RAM) or model load failed
 */
enum class AIAvailability { AVAILABLE, DOWNLOADING, DOWNLOADABLE, UNAVAILABLE }

/**
 * Context required to generate a personalised daily motivation message.
 *
 * @param todayHabits      All habits scheduled for today.
 * @param activeStreaks     Current streak per habit id (only non-zero entries required).
 * @param yesterdayCompletion  Fraction of yesterday's habits completed (0–1).
 * @param weekCompletion       Fraction of this week's habits completed so far (0–1).
 */
data class MotivationContext(
    val todayHabits: List<Habit>,
    val activeStreaks: Map<String, Int>,
    val yesterdayCompletion: Float,
    val weekCompletion: Float,
)

/**
 * Context required to generate a personalised nudge for a single habit.
 *
 * @param habit                 The habit that needs a nudge.
 * @param currentStreak         How many consecutive days completed (0 if none).
 * @param usualCompletionTime   "HH:mm" time the user typically completes this habit, or null.
 * @param currentTime           Current wall-clock time in "HH:mm" format.
 */
data class NudgeContext(
    val habit: Habit,
    val currentStreak: Int,
    val usualCompletionTime: String?,
    val currentTime: String,
)

/**
 * Central AI abstraction for Verdant.
 *
 * ## Implementations
 * | Class              | Backend                 | When used                      |
 * |--------------------|-------------------------|--------------------------------|
 * | [MediaPipeAI]      | MediaPipe LLM (Gemma 2B)| Fast / offline operations      |
 * | [FallbackAI]       | Curated templates       | No model available             |
 * | [CloudAI]          | Claude via Firebase Fn  | Deep analysis, reports, chat   |
 * | [VerdantAIRouter]  | Routes between all three| Single injected entry-point    |
 *
 * ## Routing contract (enforced in [VerdantAIRouter])
 * - **On-device** (`parseHabitDescription`, `generateNudge`, `generateMilestoneMessage`):
 *   MediaPipeAI → FallbackAI. Never requires internet.
 * - **Dual-path** (`generateMotivation`):
 *   MediaPipe runs immediately; Claude runs concurrently if online.
 *   The richer (longer) result is returned within a short timeout.
 * - **Cloud-only** (everything below the separator):
 *   Always routed to [CloudAI]. If offline, throws [AIFeatureUnavailableException]
 *   with reason `NO_NETWORK` so the UI can show "Connect for AI insights".
 *
 * All suspend functions are safe to call from any coroutine dispatcher.
 */
interface VerdantAI {

    /**
     * Parses a free-form habit description into a [ParsedHabit] suitable for
     * pre-filling the habit creation form.
     */
    suspend fun parseHabitDescription(text: String): ParsedHabit

    /**
     * Generates a short motivational message (1–2 sentences) based on the user's
     * current habit context.
     *
     * The router tries both Gemini Nano and Claude concurrently; the richer result wins.
     */
    suspend fun generateMotivation(context: MotivationContext): String

    /**
     * Generates a brief nudge (≤ 1 sentence) reminding the user to complete a habit.
     * Always on-device — never waits for the network.
     */
    suspend fun generateNudge(context: NudgeContext): String

    /**
     * Generates a celebratory milestone message for a streak achievement.
     * Always on-device — never waits for the network.
     */
    suspend fun generateMilestoneMessage(habit: Habit, milestone: Int): String

    /**
     * Hot flow that emits the current on-device model availability status.
     * Callers can use this to show a model-download progress indicator.
     */
    fun isOnDeviceAvailable(): Flow<AIAvailability>

    /**
     * Generates an enhanced motivational message via Claude.
     * More personalised and verbose than the on-device version.
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun generateDailyMotivationEnhanced(context: MotivationContext): String =
        throw AIFeatureUnavailableException.noNetwork()

    /**
     * Generates a structured 7-day habit report via Claude.
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun generateWeeklyReport(data: WeeklyReportData): WeeklyReport =
        throw AIFeatureUnavailableException.noNetwork()

    /**
     * Generates a structured 30-day habit report via Claude.
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun generateMonthlyReport(data: MonthlyReportData): MonthlyReport =
        throw AIFeatureUnavailableException.noNetwork()

    /**
     * Detects significant patterns in habit data via Claude.
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun findPatterns(data: PatternData): List<Pattern> =
        throw AIFeatureUnavailableException.noNetwork()

    /**
     * Finds correlations between pairs of habits via Claude.
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun findCorrelations(data: CorrelationData): List<Correlation> =
        throw AIFeatureUnavailableException.noNetwork()

    /**
     * Sends a multi-turn conversation to the AI coach with habit context.
     *
     * @param messages   Conversation history (earliest first).
     * @param habitData  Compact context about the user's recent habit performance.
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun chatWithCoach(
        messages: List<ChatMessage>,
        habitData: HabitSummary,
    ): String = throw AIFeatureUnavailableException.noNetwork()

    // ── Finance AI methods ──────────────────────────────────────

    /**
     * Categorizes a transaction based on merchant name and SMS context.
     * On-device by default; falls back to keyword rules.
     */
    suspend fun categorizeTransaction(
        merchant: String,
        amount: Double,
        smsSnippet: String,
    ): String = "OTHER"

    /**
     * Predicts next month's spending using historical data via Cloud AI.
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun predictMonthlySpending(history: FinanceHistory): com.verdant.core.model.MonthlyPrediction =
        throw AIFeatureUnavailableException.noNetwork()

    /**
     * Generates an AI insight about spending patterns via Cloud AI.
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun generateSpendingInsight(data: SpendingSummaryData): String =
        throw AIFeatureUnavailableException.noNetwork()

    /**
     * Generates a cross-product dashboard insight combining habits + finance via Cloud AI.
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun generateDashboardInsight(context: DashboardContext): String =
        throw AIFeatureUnavailableException.noNetwork()
}
