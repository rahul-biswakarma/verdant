package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.ai.AIFeatureUnavailableException
import com.verdant.core.ai.VerdantAI
import com.verdant.core.common.PredictionDataAggregator
import com.verdant.core.common.PredictionResult
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.model.Prediction
import com.verdant.core.model.PredictionType
import com.verdant.core.model.repository.PredictionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Daily worker that aggregates all-domain data, runs statistical scorers,
 * and calls Claude for rich personalised predictions. Falls back to
 * statistical-only predictions when AI is unavailable.
 */
@HiltWorker
class PredictionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val verdantAI: VerdantAI,
    private val aggregator: PredictionDataAggregator,
    private val predictionRepository: PredictionRepository,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_predictions"
        private const val PREDICTION_TTL_MS = 24 * 60 * 60 * 1_000L // 24 hours
    }

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()

        // Clean up expired predictions first
        predictionRepository.deleteExpired(now)

        // Aggregate all-domain data + statistical scores
        val context = try {
            aggregator.aggregate()
        } catch (e: Exception) {
            // If aggregation fails (e.g., no data), exit gracefully
            return Result.success()
        }

        val expiresAt = now + PREDICTION_TTL_MS

        // Try AI-enhanced predictions, fall back to statistical-only
        val dataSharingEnabled = prefs.llmDataSharing.first()
        val result: PredictionResult? = if (dataSharingEnabled) {
            try {
                verdantAI.generatePredictions(context)
            } catch (_: AIFeatureUnavailableException) {
                null
            } catch (_: Exception) {
                null
            }
        } else null

        // Build predictions — AI-enhanced or statistical fallback
        val stats = context.statisticalPredictions

        val spendingPrediction = Prediction(
            id = UUID.randomUUID().toString(),
            predictionType = PredictionType.SPENDING_FORECAST,
            targetPeriod = "next_month",
            predictionData = result?.spending?.let {
                "${it.summary}\n\n${it.details}"
            } ?: buildSpendingFallback(context),
            confidence = result?.spending?.confidence ?: stats.spendingConfidence,
            generatedAt = now,
            expiresAt = expiresAt,
        )

        val habitPrediction = Prediction(
            id = UUID.randomUUID().toString(),
            predictionType = PredictionType.HABIT_SUSTAINABILITY,
            targetPeriod = "next_week",
            predictionData = result?.habits?.let {
                "${it.summary}\n\n${it.details}"
            } ?: buildHabitFallback(context),
            confidence = result?.habits?.confidence ?: 0.6f,
            generatedAt = now,
            expiresAt = expiresAt,
        )

        val healthPrediction = Prediction(
            id = UUID.randomUUID().toString(),
            predictionType = PredictionType.HEALTH_TRAJECTORY,
            targetPeriod = "next_week",
            predictionData = result?.health?.let {
                "${it.summary}\n\n${it.details}"
            } ?: buildHealthFallback(context),
            confidence = result?.health?.confidence ?: 0.5f,
            generatedAt = now,
            expiresAt = expiresAt,
        )

        val lifePrediction = Prediction(
            id = UUID.randomUUID().toString(),
            predictionType = PredictionType.LIFE_FORECAST,
            targetPeriod = "next_week",
            predictionData = result?.life?.let {
                "${it.summary}\n\n${it.details}"
            } ?: buildLifeFallback(context),
            confidence = result?.life?.confidence ?: 0.4f,
            generatedAt = now,
            expiresAt = expiresAt,
        )

        // Persist all predictions
        predictionRepository.insert(spendingPrediction)
        predictionRepository.insert(habitPrediction)
        predictionRepository.insert(healthPrediction)
        predictionRepository.insert(lifePrediction)

        return Result.success()
    }

    // ── Statistical fallback formatters ──────────────────────────────────────

    private fun buildSpendingFallback(
        context: com.verdant.core.common.PredictionContext,
    ): String {
        val finance = context.financeSummary
        val forecast = context.statisticalPredictions.spendingForecast
        val topCats = finance.topCategories.take(3).joinToString(", ") { "${it.first}: ₹${it.second.toInt()}" }
        return "Predicted spending next month: ₹${forecast.toInt()} (trend: ${finance.spendingTrend}). Top categories: $topCats."
    }

    private fun buildHabitFallback(
        context: com.verdant.core.common.PredictionContext,
    ): String {
        val top = context.statisticalPredictions.habitSustainability.entries
            .sortedByDescending { it.value }
            .take(3)
        val habitData = context.habitData
        val summaries = top.mapNotNull { (id, score) ->
            val name = habitData.habits.firstOrNull { it.id == id }?.name ?: return@mapNotNull null
            "$name: ${(score * 100).toInt()}%"
        }
        return "Habit sustainability scores: ${summaries.joinToString(", ")}. " +
            "Overall completion this week: ${(habitData.overallCompletionThisWeek * 100).toInt()}%."
    }

    private fun buildHealthFallback(
        context: com.verdant.core.common.PredictionContext,
    ): String {
        val health = context.healthSummary
        val trajectory = context.statisticalPredictions.healthTrajectory
        return "Steps trending to ${trajectory["steps"]?.toInt() ?: "N/A"}/day. " +
            "Sleep avg: ${String.format("%.1f", health.avgSleepHours7d)}h. " +
            "Exercise: ${health.exerciseMinutes7d.toInt()} min this week. " +
            "Weight: ${health.weightTrend}."
    }

    private fun buildLifeFallback(
        context: com.verdant.core.common.PredictionContext,
    ): String {
        val stress = context.statisticalPredictions.stressIndex
        val financial = context.statisticalPredictions.financialHealthScore
        val emotional = context.emotionalSummary
        val parts = mutableListOf<String>()
        if (stress != null) parts.add("Stress index: ${(stress * 100).toInt()}%")
        if (financial != null) parts.add("Financial health: $financial/100")
        parts.add("Dominant mood: ${emotional.dominantMood7d}")
        parts.add("Energy: ${emotional.avgEnergy7d.toInt()}/10")
        return parts.joinToString(". ") + "."
    }
}
