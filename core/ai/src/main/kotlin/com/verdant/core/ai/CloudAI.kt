package com.verdant.core.ai

import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.common.AggregatedHabitData
import com.verdant.core.network.ApiErrorBody
import com.verdant.core.network.InsightRequest
import com.verdant.core.network.ReportRequest
import com.verdant.core.network.VerdantApiException
import com.verdant.core.network.VerdantApiService
import com.verdant.core.model.ChatMessage
import com.verdant.core.model.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [VerdantAI] implementation that calls the Verdant Firebase Functions backend
 * proxy, which in turn calls Claude (Haiku model) for deep analysis.
 *
 * Responsibilities:
 *  - Serialises [AggregatedHabitData] to compact [JsonObject] for the API payload.
 *  - Maps HTTP / network errors to [VerdantApiException] subtypes.
 *  - Implements only the cloud-only methods; on-device methods delegate to [FallbackAI]
 *    (the router avoids calling them here, but the fallback is safe).
 */
@Singleton
class CloudAI @Inject constructor(
    private val apiService: VerdantApiService,
    private val fallbackAI: FallbackAI,
    private val json: Json,
) : VerdantAI {
    override suspend fun parseHabitDescription(text: String): ParsedHabit =
        fallbackAI.parseHabitDescription(text)

    override suspend fun generateMotivation(context: MotivationContext): String =
        fallbackAI.generateMotivation(context)

    override suspend fun generateNudge(context: NudgeContext): String =
        fallbackAI.generateNudge(context)

    override suspend fun generateMilestoneMessage(habit: Habit, milestone: Int): String =
        fallbackAI.generateMilestoneMessage(habit, milestone)

    override fun isOnDeviceAvailable(): Flow<AIAvailability> =
        flowOf(AIAvailability.UNAVAILABLE)
    override suspend fun generateDailyMotivationEnhanced(context: MotivationContext): String {
        // Build a minimal AggregatedHabitData from MotivationContext for daily payloads
        val habitJson = buildMinimalHabitJson(context)
        val response = callInsight("daily_motivation", habitJson)
        return response.content
    }

    override suspend fun generateWeeklyReport(data: WeeklyReportData): WeeklyReport {
        val habitJson = encodeAggregated(data.aggregatedData)
        val response = callReport("weekly", habitJson)
        return WeeklyReport(
            summary = response.summary,
            patterns = response.patterns,
            suggestions = response.suggestions,
            highlights = response.highlights,
        )
    }

    override suspend fun generateMonthlyReport(data: MonthlyReportData): MonthlyReport {
        val habitJson = encodeAggregated(data.aggregatedData)
        val response = callReport("monthly", habitJson)
        return MonthlyReport(
            summary = response.summary,
            patterns = response.patterns,
            suggestions = response.suggestions,
            highlights = response.highlights,
        )
    }

    override suspend fun findPatterns(data: PatternData): List<Pattern> {
        val habitJson = encodeAggregated(data.aggregatedData)
        val response = callInsight("pattern", habitJson)
        return listOf(
            Pattern(
                description = response.content,
                confidence = response.confidence,
                relatedHabitIds = response.relatedHabitIds,
            ),
        )
    }

    override suspend fun findCorrelations(data: CorrelationData): List<Correlation> {
        val habitJson = encodeAggregated(data.aggregatedData)
        val response = callInsight("correlation", habitJson)

        // The backend returns one correlation description; we parse habit IDs from the
        // relatedHabitIds list (expects exactly 2 for a correlation).
        val ids = response.relatedHabitIds
        val name1 = data.habits.firstOrNull { it.id == ids.getOrNull(0) }?.name ?: ids.getOrNull(0) ?: ""
        val name2 = data.habits.firstOrNull { it.id == ids.getOrNull(1) }?.name ?: ids.getOrNull(1) ?: ""

        return if (ids.size >= 2) {
            listOf(
                Correlation(
                    habit1Id = ids[0],
                    habit2Id = ids[1],
                    habit1Name = name1,
                    habit2Name = name2,
                    strength = response.confidence,
                    description = response.content,
                ),
            )
        } else {
            emptyList()
        }
    }

    override suspend fun chatWithCoach(
        messages: List<ChatMessage>,
        habitData: HabitSummary,
    ): String {
        // Build a compact context payload from HabitSummary
        val habitJson = buildCoachContextJson(habitData)
        // Send the last user message as the "message" field; history is summarised in habitData
        val lastUserMessage = messages.lastOrNull { it.role == "user" }?.content ?: ""
        val response = callInsight("coach_reply", habitJson, message = lastUserMessage)
        return response.content
    }
    private suspend fun callInsight(
        type: String,
        habitJson: JsonObject,
        message: String? = null,
    ) = wrapErrors {
        apiService.generateInsight(
            InsightRequest(type = type, habitData = habitJson, message = message),
        )
    }

    private suspend fun callReport(
        type: String,
        habitJson: JsonObject,
    ) = wrapErrors {
        apiService.generateReport(
            ReportRequest(type = type, habitData = habitJson),
        )
    }

    /**
     * Executes a network call and maps all HTTP / IO errors to typed
     * [VerdantApiException] subtypes.
     */
    private suspend fun <T> wrapErrors(block: suspend () -> Response<T>): T {
        val response = try {
            block()
        } catch (e: IOException) {
            throw VerdantApiException.NetworkException(e)
        }

        if (response.isSuccessful) {
            return response.body()
                ?: throw VerdantApiException.ServerException(response.code(), "Empty response body")
        }

        // Parse error body for extra context
        val errorBody = runCatching {
            response.errorBody()?.string()?.let { json.decodeFromString<ApiErrorBody>(it) }
        }.getOrNull()

        throw when (response.code()) {
            401, 403 -> VerdantApiException.AuthException(
                errorBody?.error ?: "Authentication failed",
            )
            429 -> VerdantApiException.RateLimitException()
            else -> VerdantApiException.ServerException(response.code(), errorBody?.error)
        }
    }

    /** Serialises [AggregatedHabitData] to a [JsonObject] for API transport. */
    private fun encodeAggregated(data: AggregatedHabitData): JsonObject =
        json.encodeToString(data).let { json.parseToJsonElement(it).jsonObject }

    /**
     * Builds a minimal JSON object from a [MotivationContext] for daily motivation
     * calls where full [AggregatedHabitData] isn't available.
     */
    private fun buildMinimalHabitJson(context: MotivationContext): JsonObject {
        val habitsArray = context.todayHabits.joinToString(",") { habit ->
            val streak = context.activeStreaks[habit.id] ?: 0
            """{"id":"${habit.id}","name":"${habit.name.sanitise()}","icon":"${habit.icon}","trackingType":"${habit.trackingType.name}","completionRate":0,"currentStreak":$streak}"""
        }
        val raw = """{
          "habits":[$habitsArray],
          "overallCompletionToday":${context.yesterdayCompletion},
          "overallCompletionThisWeek":${context.weekCompletion},
          "overallCompletionThisMonth":${context.weekCompletion},
          "topStreaks":[],
          "periodDays":7
        }"""
        return json.parseToJsonElement(raw.trimIndent()).jsonObject
    }

    /** Builds a compact context JSON from a [HabitSummary] for coach chat calls. */
    private fun buildCoachContextJson(summary: HabitSummary): JsonObject {
        val habitsArray = summary.habits.joinToString(",") { habit ->
            val streak = summary.activeStreaks[habit.id] ?: 0
            """{"id":"${habit.id}","name":"${habit.name.sanitise()}","icon":"${habit.icon}","trackingType":"${habit.trackingType.name}","completionRate":${summary.recentCompletionRate},"currentStreak":$streak}"""
        }
        val raw = """{
          "habits":[$habitsArray],
          "overallCompletionToday":0,
          "overallCompletionThisWeek":${summary.recentCompletionRate},
          "overallCompletionThisMonth":${summary.recentCompletionRate},
          "topStreaks":[],
          "periodDays":${summary.periodDays}
        }"""
        return json.parseToJsonElement(raw.trimIndent()).jsonObject
    }

    /** Escapes double-quotes inside a string so it is safe to embed in a JSON literal. */
    private fun String.sanitise(): String = replace("\"", "\\\"")
}
