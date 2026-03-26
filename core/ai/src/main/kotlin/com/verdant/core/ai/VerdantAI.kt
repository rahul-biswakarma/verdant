package com.verdant.core.ai

import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.model.ChatMessage
import com.verdant.core.model.Habit
import kotlinx.coroutines.flow.Flow

// ── Supporting types ─────────────────────────────────────────────────────────

/**
 * Availability status of the on-device Gemini Nano model.
 *
 * Maps directly to ML Kit GenAI FeatureStatus values:
 *   AVAILABLE     – model is downloaded and ready for inference
 *   DOWNLOADING   – model download is currently in progress
 *   DOWNLOADABLE  – device is supported; model can be downloaded
 *   UNAVAILABLE   – device is not supported or library is absent
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

// ── Interface ────────────────────────────────────────────────────────────────

/**
 * Central AI abstraction for Verdant.
 *
 * ## Implementations
 * | Class              | Backend                 | When used                      |
 * |--------------------|-------------------------|--------------------------------|
 * | [GeminiNanoAI]     | ML Kit on-device model  | Fast / offline operations      |
 * | [FallbackAI]       | Curated templates       | No model available             |
 * | [CloudAI]          | Claude via Firebase Fn  | Deep analysis, reports, chat   |
 * | [VerdantAIRouter]  | Routes between all three| Single injected entry-point    |
 *
 * ## Routing contract (enforced in [VerdantAIRouter])
 * - **On-device** (`parseHabitDescription`, `generateNudge`, `generateMilestoneMessage`):
 *   Gemini Nano → FallbackAI. Never requires internet.
 * - **Dual-path** (`generateMotivation`):
 *   Gemini Nano runs immediately; Claude runs concurrently if online.
 *   The richer (longer) result is returned within a short timeout.
 * - **Cloud-only** (everything below the separator):
 *   Always routed to [CloudAI]. If offline, throws [AIFeatureUnavailableException]
 *   with reason `NO_NETWORK` so the UI can show "Connect for AI insights".
 *
 * All suspend functions are safe to call from any coroutine dispatcher.
 */
interface VerdantAI {

    // ── On-device methods ─────────────────────────────────────────────────────

    /**
     * Parses a free-form habit description into a [ParsedHabit] suitable for
     * pre-filling the habit creation form.
     */
    suspend fun parseHabitDescription(text: String): ParsedHabit

    /**
     * Parses a natural-language activity log (e.g. "Practiced guitar 20 min, took
     * supplements, skipped cycling because I was tired") against the user's current
     * [habits] and returns one [BrainDumpResult] per recognised habit.
     *
     * Always on-device — never requires internet.
     */
    suspend fun parseBrainDump(text: String, habits: List<Habit>): List<BrainDumpResult>

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
     * Parses a free-form "brain dump" into a list of habit log actions.
     *
     * The [habits] list is provided so the AI can match mentions to real habit names
     * rather than guessing. Always on-device — never requires internet.
     *
     * Example input: "Practiced guitar for 20 min, took supplements, skipped cycling because tired"
     */
    suspend fun parseBrainDump(text: String, habits: List<Habit>): ParsedBrainDump

    /**
     * Hot flow that emits the current on-device model availability status.
     * Callers can use this to show a model-download progress indicator.
     */
    fun isOnDeviceAvailable(): Flow<AIAvailability>

    // ── Cloud-only methods (require internet) ─────────────────────────────────
    // Default implementations throw [AIFeatureUnavailableException] so that
    // GeminiNanoAI / FallbackAI do not need to override them.

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
     * The coach uses COM-B diagnostics and Motivational Interviewing tone to address
     * the user's specific behavioural barrier (Capability, Opportunity, or Motivation).
     *
     * @param messages   Conversation history (earliest first).
     * @param habitData  Compact context about the user's recent habit performance.
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun chatWithCoach(
        messages: List<ChatMessage>,
        habitData: HabitSummary,
    ): String = throw AIFeatureUnavailableException.noNetwork()

    /**
     * Generates a Fogg habit-stack formula pairing a consistent anchor habit with a
     * new or struggling target habit.
     *
     * Example output: "After I make my morning coffee, I will do 2 minutes of stretching.
     * Both habits share a natural morning window, making the anchor cue reliable."
     *
     * Only called for habits where [HabitStackContext.anchorCompletionRate] ≥ 0.9 (90%+).
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun generateHabitStackSuggestion(context: HabitStackContext): String =
        throw AIFeatureUnavailableException.noNetwork()

    /**
     * Generates a weekly cross-domain behavioral synthesis insight.
     *
     * Analyses correlations between habits and contextual signals (stress, energy,
     * missed reasons) to produce a specific, MI-toned observation such as:
     * "On days you skip supplements AND stress is high, your cycling drops 40%."
     *
     * @throws AIFeatureUnavailableException if offline or rate-limited.
     */
    suspend fun generateBehavioralSynthesis(data: BehavioralSynthesisData): BehavioralSynthesis =
        throw AIFeatureUnavailableException.noNetwork()
}
