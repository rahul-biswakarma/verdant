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
 * | parseHabitDescription               | MediaPipeAI      | FallbackAI      |
 * | generateNudge / generateMilestone   | MediaPipeAI      | FallbackAI      |
 * | generateMotivation                  | MediaPipe+Claude†| FallbackAI      |
 * | generateDailyMotivationEnhanced     | CloudAI          | error: NO_NET   |
 * | generateWeeklyReport                | CloudAI          | error: NO_NET   |
 * | generateMonthlyReport               | CloudAI          | error: NO_NET   |
 * | findPatterns / findCorrelations     | CloudAI          | error: NO_NET   |
 * | chatWithCoach                       | CloudAI          | error: NO_NET   |
 *
 * † **Dual-path motivation**: MediaPipe and Claude run concurrently; the richer
 * (longer) result is returned if Claude responds within [CLOUD_MOTIVATION_TIMEOUT_MS].
 * If the device is offline or Claude fails, the MediaPipe result is used.
 */
@Singleton
class VerdantAIRouter @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val mediaPipeAI: MediaPipeAI,
    private val fallbackAI: FallbackAI,
    private val cloudAI: CloudAI,
) : VerdantAI {


    override suspend fun parseHabitDescription(text: String): ParsedHabit =
        mediaPipeAI.parseHabitDescription(text)

    override suspend fun generateNudge(context: NudgeContext): String =
        mediaPipeAI.generateNudge(context)

    override suspend fun generateMilestoneMessage(habit: Habit, milestone: Int): String =
        mediaPipeAI.generateMilestoneMessage(habit, milestone)

    override fun isOnDeviceAvailable(): Flow<AIAvailability> =
        mediaPipeAI.isOnDeviceAvailable()


    /**
     * Runs MediaPipe and Claude concurrently. Returns whichever produces the longer
     * (richer) message, subject to a [CLOUD_MOTIVATION_TIMEOUT_MS] timeout for Claude.
     *
     * The on-device result is always available immediately; the cloud call is a
     * best-effort enhancement.
     */
    override suspend fun generateMotivation(context: MotivationContext): String =
        coroutineScope {
            // Start on-device immediately — always succeeds (may fall back to templates)
            val localDeferred = async {
                runCatching { mediaPipeAI.generateMotivation(context) }.getOrNull()
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

            val localResult = localDeferred.await()
            val cloudResult = cloudDeferred?.await()

            // Prefer the longer result; fall back through the chain
            selectRicher(cloudResult, localResult)
                ?: fallbackAI.generateMotivation(context)
        }


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

    // ── Finance AI routing ───────────────────────────────────────

    override suspend fun categorizeTransaction(
        merchant: String,
        amount: Double,
        smsSnippet: String,
    ): String = runCatching {
        mediaPipeAI.categorizeTransaction(merchant, amount, smsSnippet)
    }.getOrDefault("OTHER")

    override suspend fun predictMonthlySpending(
        history: FinanceHistory,
    ): com.verdant.core.model.MonthlyPrediction {
        requireNetwork()
        return cloudAI.predictMonthlySpending(history)
    }

    override suspend fun generateSpendingInsight(data: SpendingSummaryData): String {
        requireNetwork()
        return cloudAI.generateSpendingInsight(data)
    }

    override suspend fun generateDashboardInsight(context: DashboardContext): String {
        requireNetwork()
        return cloudAI.generateDashboardInsight(context)
    }


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
        /** Maximum time to wait for Claude's motivation response before using local result. */
        const val CLOUD_MOTIVATION_TIMEOUT_MS = 4_000L
    }
}
