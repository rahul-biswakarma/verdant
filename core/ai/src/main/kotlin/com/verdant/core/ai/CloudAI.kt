package com.verdant.core.ai

import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.common.AggregatedHabitData
import com.verdant.core.common.PredictionContext
import com.verdant.core.common.PredictionItem
import com.verdant.core.common.PredictionResult
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

    // ── On-device methods — delegate to FallbackAI (never called by the router) ──

    override suspend fun parseHabitDescription(text: String): ParsedHabit =
        fallbackAI.parseHabitDescription(text)

    override suspend fun parseBrainDump(text: String, habits: List<Habit>): ParsedBrainDump =
        fallbackAI.parseBrainDump(text, habits)

    override suspend fun generateMotivation(context: MotivationContext): String =
        fallbackAI.generateMotivation(context)

    override suspend fun generateNudge(context: NudgeContext): String =
        fallbackAI.generateNudge(context)

    override suspend fun generateMilestoneMessage(habit: Habit, milestone: Int): String =
        fallbackAI.generateMilestoneMessage(habit, milestone)

    override fun isOnDeviceAvailable(): Flow<AIAvailability> =
        flowOf(AIAvailability.UNAVAILABLE)

    // ── Cloud-only methods ────────────────────────────────────────────────────

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

    override suspend fun generateHabitStackSuggestion(context: HabitStackContext): String {
        val habitJson = buildHabitStackContextJson(context)
        val stackJson = buildStackContextJson(context)
        val response = callInsightWithStack("habit_stack", habitJson, stackContext = stackJson)
        return response.content
    }

    override suspend fun generateBehavioralSynthesis(data: BehavioralSynthesisData): BehavioralSynthesis {
        val habitJson = buildBehavioralSynthesisJson(data)
        val response = callInsight("weekly_behavioral_synthesis", habitJson)
        return BehavioralSynthesis(
            insight = response.content,
            relatedHabitIds = response.relatedHabitIds,
        )
    }

    override suspend fun generatePredictions(context: PredictionContext): PredictionResult {
        val dataJson = buildPredictionContextJson(context)
        val response = callInsight("daily_predictions", dataJson)
        return parsePredictionResponse(response.content, response.confidence)
    }

    override suspend fun analyzeStory(context: StoryAnalysisContext): StoryAnalysisResult {
        val dataJson = buildStoryAnalysisJson(context)
        val response = callInsight("story_analysis", dataJson)
        return parseStoryAnalysisResponse(response.content)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private suspend fun callInsight(
        type: String,
        habitJson: JsonObject,
        message: String? = null,
    ) = wrapErrors {
        apiService.generateInsight(
            InsightRequest(type = type, habitData = habitJson, message = message),
        )
    }

    private suspend fun callInsightWithStack(
        type: String,
        habitJson: JsonObject,
        stackContext: JsonObject,
    ) = wrapErrors {
        apiService.generateInsight(
            InsightRequest(type = type, habitData = habitJson, stackContext = stackContext),
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

    /**
     * Builds a minimal JSON object representing a [HabitStackContext] as the main
     * habit data payload for habit_stack calls (contains both anchor + target habits).
     */
    private fun buildHabitStackContextJson(context: HabitStackContext): JsonObject {
        val habits = listOf(context.anchorHabit, context.targetHabit)
        val habitsArray = habits.joinToString(",") { habit ->
            val rate = if (habit.id == context.anchorHabit.id) context.anchorCompletionRate
                       else context.targetCompletionRate
            """{"id":"${habit.id}","name":"${habit.name.sanitise()}","icon":"${habit.icon}","trackingType":"${habit.trackingType.name}","completionRate":$rate,"currentStreak":0}"""
        }
        val raw = """{
          "habits":[$habitsArray],
          "overallCompletionToday":0,
          "overallCompletionThisWeek":${context.anchorCompletionRate},
          "overallCompletionThisMonth":${context.anchorCompletionRate},
          "topStreaks":[],
          "periodDays":14
        }"""
        return json.parseToJsonElement(raw.trimIndent()).jsonObject
    }

    /** Serialises the [HabitStackContext] anchor/target pairing as the stackContext field. */
    private fun buildStackContextJson(context: HabitStackContext): JsonObject {
        val timeField = context.anchorConsistentTime
            ?.let { ""","anchorConsistentTime":"$it"""" }
            ?: ""
        val raw = """{
          "anchorHabitId":"${context.anchorHabit.id}",
          "anchorHabitName":"${context.anchorHabit.name.sanitise()}",
          "anchorHabitIcon":"${context.anchorHabit.icon}",
          "anchorCompletionRate":${context.anchorCompletionRate}$timeField,
          "targetHabitId":"${context.targetHabit.id}",
          "targetHabitName":"${context.targetHabit.name.sanitise()}",
          "targetHabitIcon":"${context.targetHabit.icon}",
          "targetCompletionRate":${context.targetCompletionRate}
        }"""
        return json.parseToJsonElement(raw.trimIndent()).jsonObject
    }

    /**
     * Builds the JSON payload for [generateBehavioralSynthesis], enriching each
     * [HabitSummaryItem] with contextual signals when available.
     */
    private fun buildBehavioralSynthesisJson(data: BehavioralSynthesisData): JsonObject {
        val habitsArray = data.habits.joinToString(",") { habit ->
            val signals = data.contextualSignals[habit.id]
            val stressField = signals?.avgStressOnMiss
                ?.let { ""","avgStressOnMiss":$it""" } ?: ""
            val energyField = signals?.avgEnergyOnComplete
                ?.let { ""","avgEnergyOnComplete":$it""" } ?: ""
            val missField = signals?.topMissedReason
                ?.let { ""","topMissedReason":"${it.sanitise()}"""" } ?: ""
            val summaryItem = data.aggregatedData.habits.firstOrNull { it.id == habit.id }
            val rate = summaryItem?.completionRate ?: 0f
            val streak = summaryItem?.currentStreak ?: 0
            """{"id":"${habit.id}","name":"${habit.name.sanitise()}","icon":"${habit.icon}","trackingType":"${habit.trackingType.name}","completionRate":$rate,"currentStreak":$streak$stressField$energyField$missField}"""
        }
        val raw = """{
          "habits":[$habitsArray],
          "overallCompletionToday":${data.aggregatedData.overallCompletionToday},
          "overallCompletionThisWeek":${data.aggregatedData.overallCompletionThisWeek},
          "overallCompletionThisMonth":${data.aggregatedData.overallCompletionThisMonth},
          "topStreaks":[],
          "periodDays":${data.periodDays}
        }"""
        return json.parseToJsonElement(raw.trimIndent()).jsonObject
    }

    /** Builds a compact JSON payload from [PredictionContext] for the daily_predictions call. */
    private fun buildPredictionContextJson(context: PredictionContext): JsonObject {
        val topCats = context.financeSummary.topCategories.joinToString(",") { (cat, amt) ->
            """{"category":"${cat.sanitise()}","amount":$amt}"""
        }
        val habitSust = context.statisticalPredictions.habitSustainability.entries
            .sortedByDescending { it.value }
            .take(5)
            .joinToString(",") { (id, score) ->
                val name = context.habitData.habits.firstOrNull { it.id == id }?.name ?: id
                """{"name":"${name.sanitise()}","score":$score}"""
            }
        val moodDist = context.emotionalSummary.moodDistribution.entries.joinToString(",") { (mood, count) ->
            """"$mood":$count"""
        }
        val actBreakdown = context.activitySummary.activityBreakdown.entries.joinToString(",") { (type, count) ->
            """"$type":$count"""
        }
        val raw = """{
          "habits":{"completionToday":${context.habitData.overallCompletionToday},"completionWeek":${context.habitData.overallCompletionThisWeek},"completionMonth":${context.habitData.overallCompletionThisMonth},"topStreaks":[${context.habitData.topStreaks.joinToString(",") { """{"name":"${it.habitName.sanitise()}","streak":${it.currentStreak}}""" }}],"sustainability":[$habitSust]},
          "finance":{"monthlySpent":${context.financeSummary.monthlySpent},"monthlyIncome":${context.financeSummary.monthlyIncome},"topCategories":[$topCats],"trend":"${context.financeSummary.spendingTrend}","predictedNextMonth":${context.financeSummary.predictedNextMonth ?: 0}},
          "health":{"avgSteps":${context.healthSummary.avgSteps7d},"avgSleep":${context.healthSummary.avgSleepHours7d},"avgHeartRate":${context.healthSummary.avgHeartRate7d},"exerciseMin":${context.healthSummary.exerciseMinutes7d},"weightTrend":"${context.healthSummary.weightTrend}"},
          "emotional":{"dominantMood":"${context.emotionalSummary.dominantMood7d}","avgEnergy":${context.emotionalSummary.avgEnergy7d},"avgStress":${context.emotionalSummary.avgStress7d},"moodDist":{$moodDist}},
          "activity":{"dominant":"${context.activitySummary.dominantActivity}","total7d":${context.activitySummary.totalActivities7d},"breakdown":{$actBreakdown}},
          "device":{"avgScreenTimeMin":${context.deviceStats.avgScreenTimeMinutes7d},"avgNotifications":${context.deviceStats.avgNotifications7d}},
          "scores":{"spendingForecast":${context.statisticalPredictions.spendingForecast},"spendingConfidence":${context.statisticalPredictions.spendingConfidence},"stressIndex":${context.statisticalPredictions.stressIndex ?: -1},"financialHealth":${context.statisticalPredictions.financialHealthScore ?: -1},"lifestyleScore":${context.statisticalPredictions.lifestyleScore ?: -1}}
        }"""
        return json.parseToJsonElement(raw.trimIndent()).jsonObject
    }

    /**
     * Parses a prediction response from Claude into a structured [PredictionResult].
     * Claude returns a JSON-like response; we extract four sections.
     */
    private fun parsePredictionResponse(content: String, confidence: Float): PredictionResult {
        // Try JSON parse first; fall back to single-text split
        return try {
            val parsed = json.parseToJsonElement(content).jsonObject
            PredictionResult(
                spending = extractPredictionItem(parsed, "spending", confidence),
                habits = extractPredictionItem(parsed, "habits", confidence),
                health = extractPredictionItem(parsed, "health", confidence),
                life = extractPredictionItem(parsed, "life", confidence),
            )
        } catch (_: Exception) {
            // Fallback: wrap entire content as a life prediction
            val item = PredictionItem(
                summary = content.take(200),
                details = content,
                confidence = confidence,
                keyInsights = emptyList(),
            )
            PredictionResult(spending = item, habits = item, health = item, life = item)
        }
    }

    private fun extractPredictionItem(obj: JsonObject, key: String, fallbackConfidence: Float): PredictionItem {
        val section = obj[key]?.jsonObject
        return PredictionItem(
            summary = section?.get("summary")?.toString()?.trim('"') ?: "",
            details = section?.get("details")?.toString()?.trim('"') ?: "",
            confidence = section?.get("confidence")?.toString()?.toFloatOrNull() ?: fallbackConfidence,
            keyInsights = section?.get("keyInsights")?.let { arr ->
                try {
                    json.decodeFromString<List<String>>(arr.toString())
                } catch (_: Exception) { emptyList() }
            } ?: emptyList(),
        )
    }

    /** Builds a compact JSON payload from [StoryAnalysisContext] for story_analysis calls. */
    private fun buildStoryAnalysisJson(context: StoryAnalysisContext): JsonObject {
        val eventsArray = context.events.joinToString(",") { event ->
            """{"type":"${event.type}","title":"${event.title.sanitise()}","timestamp":${event.timestamp}}"""
        }
        val raw = """{
          "storyTitle":"${context.storyTitle.sanitise()}",
          "template":"${context.template ?: "CUSTOM"}",
          "durationMinutes":${context.durationMinutes},
          "events":[$eventsArray]
        }"""
        return json.parseToJsonElement(raw.trimIndent()).jsonObject
    }

    /** Parses a story analysis response from Claude. */
    private fun parseStoryAnalysisResponse(content: String): StoryAnalysisResult {
        return try {
            val parsed = json.parseToJsonElement(content).jsonObject
            StoryAnalysisResult(
                summary = parsed["summary"]?.toString()?.trim('"') ?: content.take(200),
                behavioralInsights = parsed["behavioralInsights"]?.let {
                    json.decodeFromString<List<String>>(it.toString())
                } ?: emptyList(),
                patterns = parsed["patterns"]?.let {
                    json.decodeFromString<List<String>>(it.toString())
                } ?: emptyList(),
                suggestedActions = parsed["suggestedActions"]?.let {
                    json.decodeFromString<List<String>>(it.toString())
                } ?: emptyList(),
            )
        } catch (_: Exception) {
            StoryAnalysisResult(
                summary = content.take(200),
                behavioralInsights = listOf(content),
                patterns = emptyList(),
                suggestedActions = emptyList(),
            )
        }
    }

    /** Escapes double-quotes inside a string so it is safe to embed in a JSON literal. */
    private fun String.sanitise(): String = replace("\"", "\\\"")
}
