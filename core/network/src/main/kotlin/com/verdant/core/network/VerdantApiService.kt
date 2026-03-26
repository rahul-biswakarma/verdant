package com.verdant.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the Verdant Firebase Functions backend proxy.
 *
 * Base URL is the Cloud Functions HTTPS endpoint configured in [di.NetworkModule].
 *
 * All methods return [Response] wrappers so callers can inspect HTTP error
 * codes directly and map them to [VerdantApiException] subtypes.
 *
 * Authentication is injected transparently by [AuthInterceptor].
 */
interface VerdantApiService {

    /**
     * Generates a short AI insight (motivation, pattern, nudge, or coach reply).
     *
     * Maps to the `generateInsight` Firebase Function.
     */
    @POST("generateInsight")
    suspend fun generateInsight(
        @Body request: InsightRequest,
    ): Response<InsightResponse>

    /**
     * Generates a structured weekly or monthly habit report.
     *
     * Maps to the `generateReport` Firebase Function.
     */
    @POST("generateReport")
    suspend fun generateReport(
        @Body request: ReportRequest,
    ): Response<ReportResponse>
}

// ── Request models ────────────────────────────────────────────────────────────

@Serializable
data class InsightRequest(
    /**
     * One of: daily_motivation | pattern | correlation | weekly_summary | monthly_summary |
     * suggestion | coach_reply | habit_stack | weekly_behavioral_synthesis
     */
    val type: String,
    /** Compact habit data payload produced by HabitDataAggregator */
    @SerialName("habitData") val habitData: JsonObject,
    /** Required only for coach_reply type */
    val message: String? = null,
    /** Required only for habit_stack type */
    @SerialName("stackContext") val stackContext: JsonObject? = null,
)

@Serializable
data class ReportRequest(
    /** "weekly" or "monthly" */
    val type: String,
    /** Compact habit data payload produced by HabitDataAggregator */
    @SerialName("habitData") val habitData: JsonObject,
)

// ── Response models ───────────────────────────────────────────────────────────

@Serializable
data class InsightResponse(
    /** The generated AI text */
    val content: String,
    /** IDs of habits explicitly referenced in the content */
    @SerialName("relatedHabitIds") val relatedHabitIds: List<String> = emptyList(),
    /** Model-reported confidence in the insight (0–1) */
    val confidence: Float = 0.5f,
)

@Serializable
data class ReportResponse(
    val summary: String,
    val patterns: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val highlights: List<String> = emptyList(),
)

// ── Error body ────────────────────────────────────────────────────────────────

/** Shape of the JSON body returned alongside 4xx/5xx HTTP errors. */
@Serializable
data class ApiErrorBody(
    val error: String? = null,
    val code: String? = null,
)
