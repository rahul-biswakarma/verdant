package com.verdant.core.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.model.ChatMessage
import com.verdant.core.model.Habit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single [VerdantAI] entry-point injected throughout the app.
 *
 * Implements three-tier routing:
 *
 * | Operation                           | Primary          | Fallback        |
 * |-------------------------------------|------------------|-----------------|
 * | parseHabitDescription               | GeminiNanoAI     | FallbackAI      |
 * | generateNudge / generateMilestone   | GeminiNanoAI     | FallbackAI      |
 * | generateMotivation                  | Gemini + Claude† | FallbackAI      |
 * | generateDailyMotivationEnhanced     | CloudAI          | error: NO_NET   |
 * | generateWeeklyReport                | CloudAI          | error: NO_NET   |
 * | generateMonthlyReport               | CloudAI          | error: NO_NET   |
 * | findPatterns / findCorrelations     | CloudAI          | error: NO_NET   |
 * | chatWithCoach                       | CloudAI          | error: NO_NET   |
 *
 * † **Dual-path motivation**: Gemini Nano and Claude run concurrently; the richer
 * (longer) result is returned if Claude responds within [CLOUD_MOTIVATION_TIMEOUT_MS].
 * If the device is offline or Claude fails, the Gemini Nano result is used.
 */
@Singleton
class VerdantAIRouter @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val geminiNanoAI: GeminiNanoAI,
    private val fallbackAI: FallbackAI,
    private val cloudAI: CloudAI,
) : VerdantAI {

    // ── On-device ─────────────────────────────────────────────────────────────

    override suspend fun parseHabitDescription(text: String): ParsedHabit =
        geminiNanoAI.parseHabitDescription(text)

    override suspend fun parseBrainDump(text: String, habits: List<Habit>): List<BrainDumpResult> =
        geminiNanoAI.parseBrainDump(text, habits)

    override suspend fun generateNudge(context: NudgeContext): String =
        geminiNanoAI.generateNudge(context)

    override suspend fun generateMilestoneMessage(habit: Habit, milestone: Int): String =
        geminiNanoAI.generateMilestoneMessage(habit, milestone)

    override fun isOnDeviceAvailable(): Flow<AIAvailability> =
        geminiNanoAI.isOnDeviceAvailable()

    // ── Dual-path motivation ──────────────────────────────────────────────────

    /**
     * Runs Gemini Nano and Claude concurrently. Returns whichever produces the longer
     * (richer) message, subject to a [CLOUD_MOTIVATION_TIMEOUT_MS] timeout for Claude.
     *
     * The on-device result is always available immediately; the cloud call is a
     * best-effort enhancement.
     */
    override suspend fun generateMotivation(context: MotivationContext): String =
        coroutineScope {
            // Start on-device immediately — always succeeds (may fall back to templates)
            val geminiDeferred = async {
                runCatching { geminiNanoAI.generateMotivation(context) }.getOrNull()
            }

            // Only start cloud call if we have a real network connection
            val cloudDeferred = if (isNetworkAvailable()) {
                async {
                    withTimeoutOrNull(CLOUD_MOTIVATION_TIMEOUT_MS) {
                        runCatching { cloudAI.generateDailyMotivationEnhanced(context) }
                            .getOrNull()
                            ?.takeIf { it.isNotBlank() }
                    }
                }
            } else null

            val geminiResult = geminiDeferred.await()
            val cloudResult = cloudDeferred?.await()

            // Prefer the longer result; fall back through the chain
            selectRicher(cloudResult, geminiResult)
                ?: fallbackAI.generateMotivation(context)
        }

    // ── Cloud-only — guard and delegate ──────────────────────────────────────

    override suspend fun generateDailyMotivationEnhanced(context: MotivationContext): String {
        requireNetwork()
        return cloudAI.generateDailyMotivationEnhanced(context)
    }

    override suspend fun generateWeeklyReport(data: WeeklyReportData): WeeklyReport {
        requireNetwork()
        return cloudAI.generateWeeklyReport(data)
    }

    override suspend fun generateMonthlyReport(data: MonthlyReportData): MonthlyReport {
        requireNetwork()
        return cloudAI.generateMonthlyReport(data)
    }

    override suspend fun findPatterns(data: PatternData): List<Pattern> {
        requireNetwork()
        return cloudAI.findPatterns(data)
    }

    override suspend fun findCorrelations(data: CorrelationData): List<Correlation> {
        requireNetwork()
        return cloudAI.findCorrelations(data)
    }

    override suspend fun chatWithCoach(
        messages: List<ChatMessage>,
        habitData: HabitSummary,
    ): String {
        requireNetwork()
        return cloudAI.chatWithCoach(messages, habitData)
    }

    // ── Network utilities ─────────────────────────────────────────────────────

    private fun isNetworkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Throws [AIFeatureUnavailableException] with [AIFeatureUnavailableException.Reason.NO_NETWORK] if offline. */
    private fun requireNetwork() {
        if (!isNetworkAvailable()) throw AIFeatureUnavailableException.noNetwork()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the non-null string with more content (measured by character count),
     * or null if both are null/blank.
     */
    private fun selectRicher(a: String?, b: String?): String? {
        val cleanA = a?.takeIf { it.isNotBlank() }
        val cleanB = b?.takeIf { it.isNotBlank() }
        return when {
            cleanA == null -> cleanB
            cleanB == null -> cleanA
            cleanA.length >= cleanB.length -> cleanA
            else -> cleanB
        }
    }

    private companion object {
        /** Maximum time to wait for Claude's motivation response before using Gemini Nano result. */
        const val CLOUD_MOTIVATION_TIMEOUT_MS = 4_000L
    }
}
