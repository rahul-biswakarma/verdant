package com.verdant.core.common

import com.verdant.core.model.DeviceStatType
import com.verdant.core.model.HealthRecordType
import com.verdant.core.model.TransactionType
import com.verdant.core.model.repository.ActivityRecordRepository
import com.verdant.core.model.repository.DeviceStatRepository
import com.verdant.core.model.repository.EmotionalContextRepository
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.model.repository.HealthRecordRepository
import com.verdant.core.model.repository.RecurringTransactionRepository
import com.verdant.core.model.repository.StreakCacheRepository
import com.verdant.core.model.repository.TransactionRepository
import com.verdant.core.prediction.FinancialHealthScorer
import com.verdant.core.prediction.HabitSustainabilityScorer
import com.verdant.core.prediction.HealthTrajectoryPredictor
import com.verdant.core.prediction.SpendingForecaster
import com.verdant.core.prediction.StressIndexCalculator
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates data from ALL domains (habits, finance, health, device, emotional,
 * activity) plus pre-computes statistical predictions. Output is a compact
 * [PredictionContext] sized under ~2 000 tokens for the Claude API.
 */
@Singleton
class PredictionDataAggregator @Inject constructor(
    private val habitRepository: HabitRepository,
    private val habitEntryRepository: HabitEntryRepository,
    private val streakCacheRepository: StreakCacheRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val healthRecordRepository: HealthRecordRepository,
    private val emotionalContextRepository: EmotionalContextRepository,
    private val deviceStatRepository: DeviceStatRepository,
    private val activityRecordRepository: ActivityRecordRepository,
    private val habitDataAggregator: HabitDataAggregator,
    private val financeDataAggregator: FinanceDataAggregator,
    private val spendingForecaster: SpendingForecaster,
) {

    /**
     * Collects 14–30 days of data across all domains, runs statistical scorers,
     * and compresses everything into a single [PredictionContext].
     */
    suspend fun aggregate(): PredictionContext {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val sevenDaysAgo = today.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val thirtyDaysAgo = today.minusDays(30).atStartOfDay(zone).toInstant().toEpochMilli()
        val sixMonthsAgo = today.minusMonths(6).atStartOfDay(zone).toInstant().toEpochMilli()

        // ── Habits ───────────────────────────────────────────────────────────
        val habits = habitRepository.getAllHabits().filter { !it.isArchived }
        val entries = habitEntryRepository.getAllEntries()
            .filter { it.date >= today.minusDays(30) }
        val streakMap = streakCacheRepository.getAll()
            .associate { it.habitId to it.currentStreak }

        val habitData = habitDataAggregator.aggregateForDailyInsight(
            habits = habits,
            entries = entries,
            streaks = streakMap,
            today = today,
            periodDays = 14,
        )

        // ── Finance ──────────────────────────────────────────────────────────
        val allTransactions = transactionRepository.observeByDateRange(sixMonthsAgo, now).first()
        val thisMonthTxns = allTransactions.filter { txn ->
            val txnMonth = YearMonth.from(
                Instant.ofEpochMilli(txn.transactionDate).atZone(zone).toLocalDate(),
            )
            txnMonth == YearMonth.from(today)
        }
        val debits = thisMonthTxns.filter { it.type == TransactionType.DEBIT }
        val credits = thisMonthTxns.filter { it.type == TransactionType.CREDIT }
        val monthlySpent = debits.sumOf { it.amount }
        val monthlyIncome = credits.sumOf { it.amount }

        val topCategories = debits
            .groupBy { it.category ?: "OTHER" }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }

        val monthlyTotals = financeDataAggregator.monthlyTotals(allTransactions)
        val sortedMonths = monthlyTotals.entries.sortedBy { it.key }
        val spendingTrend = when {
            sortedMonths.size < 2 -> "stable"
            else -> {
                val recent = sortedMonths.takeLast(2)
                val diff = recent.last().value - recent.first().value
                when {
                    diff > recent.first().value * 0.1 -> "increasing"
                    diff < -recent.first().value * 0.1 -> "decreasing"
                    else -> "stable"
                }
            }
        }

        val recurringTxns = recurringTransactionRepository.observeActive().first()
        val prediction = financeDataAggregator.predictNextMonth(allTransactions, recurringTxns)

        val financeSummary = CompactFinanceSummary(
            monthlySpent = monthlySpent,
            monthlyIncome = monthlyIncome,
            topCategories = topCategories,
            spendingTrend = spendingTrend,
            predictedNextMonth = prediction?.predictedTotal,
        )

        // ── Health ───────────────────────────────────────────────────────────
        val healthRecords = healthRecordRepository.observeByRange(sevenDaysAgo, now).first()
        val healthByType = healthRecords.groupBy { it.recordType }

        val avgSteps = healthByType[HealthRecordType.STEPS]
            ?.map { it.value }?.average() ?: 0.0
        val avgSleep = healthByType[HealthRecordType.SLEEP]
            ?.map { it.value }?.average() ?: 0.0
        val avgHeartRate = healthByType[HealthRecordType.HEART_RATE]
            ?.map { it.value }?.average() ?: 0.0
        val exerciseMinutes = healthByType[HealthRecordType.EXERCISE]
            ?.sumOf { it.value } ?: 0.0
        val weightRecords = healthByType[HealthRecordType.WEIGHT]
            ?.sortedBy { it.recordedAt }
        val weightTrend = when {
            weightRecords == null || weightRecords.size < 2 -> "stable"
            weightRecords.last().value > weightRecords.first().value -> "increasing"
            weightRecords.last().value < weightRecords.first().value -> "decreasing"
            else -> "stable"
        }

        val healthSummary = CompactHealthSummary(
            avgSteps7d = avgSteps,
            avgSleepHours7d = avgSleep,
            avgHeartRate7d = avgHeartRate,
            exerciseMinutes7d = exerciseMinutes,
            weightTrend = weightTrend,
        )

        // ── Emotional ────────────────────────────────────────────────────────
        val emotionalRecords = emotionalContextRepository.observeByRange(sevenDaysAgo, now).first()
        val moodDistribution = emotionalRecords
            .groupBy { it.inferredMood.name }
            .mapValues { it.value.size }
        val dominantMood = moodDistribution.maxByOrNull { it.value }?.key ?: "NEUTRAL"
        val avgEnergy = emotionalRecords
            .map { it.energyLevel.toFloat() }
            .takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 5f
        // Stress is inferred from mood — STRESSED/ANXIOUS moods count as high stress
        val stressCount = emotionalRecords.count { it.inferredMood == com.verdant.core.model.InferredMood.STRESSED || it.inferredMood == com.verdant.core.model.InferredMood.ANXIOUS }
        val avgStress = if (emotionalRecords.isNotEmpty()) (stressCount.toFloat() / emotionalRecords.size * 10f) else 5f

        val emotionalSummary = CompactEmotionalSummary(
            dominantMood7d = dominantMood,
            avgEnergy7d = avgEnergy,
            avgStress7d = avgStress,
            moodDistribution = moodDistribution,
        )

        // ── Activity ─────────────────────────────────────────────────────────
        val activityRecords = activityRecordRepository.observeByRange(sevenDaysAgo, now).first()
        val activityBreakdown = activityRecords
            .groupBy { it.activityType.name }
            .mapValues { it.value.size }
        val dominantActivity = activityBreakdown.maxByOrNull { it.value }?.key ?: "STILL"

        val activitySummary = CompactActivitySummary(
            dominantActivity = dominantActivity,
            totalActivities7d = activityRecords.size,
            activityBreakdown = activityBreakdown,
        )

        // ── Device stats ─────────────────────────────────────────────────────
        val deviceStats = deviceStatRepository.observeByRange(sevenDaysAgo, now).first()
        val avgScreenTime = deviceStats
            .filter { it.statType == DeviceStatType.SCREEN_TIME }
            .map { it.value }
            .takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val avgNotifications = deviceStats
            .filter { it.statType == DeviceStatType.NOTIFICATION_COUNT }
            .map { it.value }
            .takeIf { it.isNotEmpty() }?.average() ?: 0.0

        val compactDeviceStats = CompactDeviceStats(
            avgScreenTimeMinutes7d = avgScreenTime,
            avgNotifications7d = avgNotifications,
        )

        // ── Statistical predictions ──────────────────────────────────────────
        val spendingForecast = prediction?.predictedTotal ?: 0.0
        val spendingConfidence = prediction?.confidence ?: 0.3f

        val habitSustainabilityScorer = HabitSustainabilityScorer()
        val habitSustainability = habits.associate { habit ->
            val habitEntries = entries.filter { it.habitId == habit.id }
            val streak = streakMap[habit.id] ?: 0
            val total = habitEntries.size.coerceAtLeast(1)
            val completed = habitEntries.count { it.completed }
            val completionRate = completed.toFloat() / total

            // Compute decay: compare last 7 days vs prior 7 days
            val last7 = habitEntries.filter { it.date >= today.minusDays(7) }
            val prior7 = habitEntries.filter { it.date in today.minusDays(14)..today.minusDays(8) }
            val last7Rate = if (last7.isNotEmpty()) last7.count { it.completed }.toFloat() / last7.size else 0f
            val prior7Rate = if (prior7.isNotEmpty()) prior7.count { it.completed }.toFloat() / prior7.size else last7Rate
            val decay = (prior7Rate - last7Rate).coerceIn(0f, 1f)

            // Variance: std dev of daily completion over 14 days
            val dailyRates = (0 until 14).map { offset ->
                val date = today.minusDays(offset.toLong())
                val dayEntries = habitEntries.filter { it.date == date }
                if (dayEntries.isNotEmpty()) dayEntries.count { it.completed }.toFloat() / dayEntries.size else 0f
            }
            val mean = dailyRates.average().toFloat()
            val variance = dailyRates.map { (it - mean) * (it - mean) }.average().toFloat()

            habit.id to habitSustainabilityScorer.score(
                HabitSustainabilityScorer.HabitHistory(
                    streakLength = streak,
                    completionRate = completionRate,
                    completionDecay = decay,
                    variance = variance,
                ),
            )
        }

        val healthTrajectoryPredictor = HealthTrajectoryPredictor()
        val healthTrajectory = mapOf(
            "steps" to healthTrajectoryPredictor.predict(
                healthByType[HealthRecordType.STEPS]?.map {
                    HealthTrajectoryPredictor.DataPoint(it.recordedAt, it.value)
                } ?: emptyList(),
            ),
            "sleep" to healthTrajectoryPredictor.predict(
                healthByType[HealthRecordType.SLEEP]?.map {
                    HealthTrajectoryPredictor.DataPoint(it.recordedAt, it.value)
                } ?: emptyList(),
            ),
        )

        // Stress index — only compute if we have enough baseline data
        val stressIndex = if (deviceStats.isNotEmpty() && entries.isNotEmpty()) {
            val stressCalc = StressIndexCalculator()
            val missCount = habits.size - entries.filter { it.date == today && it.completed }.size
            val sleepHours = healthByType[HealthRecordType.SLEEP]?.lastOrNull()?.value ?: avgSleep
            val spendingRatio = if (monthlyIncome > 0) monthlySpent / monthlyIncome else 0.0

            stressCalc.calculate(
                StressIndexCalculator.StressSignals(
                    screenTimeMinutes = avgScreenTime,
                    notificationCount = avgNotifications.toInt(),
                    sleepHours = sleepHours,
                    spendingRatio = spendingRatio,
                    habitMissCount = missCount.coerceAtLeast(0),
                ),
                StressIndexCalculator.BaselineStats(
                    avgScreenTime = avgScreenTime,
                    stdScreenTime = avgScreenTime * 0.2,
                    avgNotifications = avgNotifications,
                    stdNotifications = avgNotifications * 0.2,
                    avgSleep = avgSleep,
                    stdSleep = avgSleep * 0.15,
                    avgSpendingRatio = if (monthlyIncome > 0) monthlySpent / monthlyIncome else 0.5,
                    stdSpendingRatio = 0.2,
                    avgMisses = habits.size * 0.3,
                    stdMisses = habits.size * 0.15,
                ),
            )
        } else null

        // Financial health score
        val financialHealthScore = if (allTransactions.isNotEmpty()) {
            val scorer = FinancialHealthScorer()
            val allDebits = allTransactions.filter { it.type == TransactionType.DEBIT }
            val allCredits = allTransactions.filter { it.type == TransactionType.CREDIT }
            val totalIncome = allCredits.sumOf { it.amount }
            val totalSpent = allDebits.sumOf { it.amount }
            val savingsRate = if (totalIncome > 0) (totalIncome - totalSpent) / totalIncome else 0.0

            val monthlyAmounts = monthlyTotals.values.toList()
            val monthlyMean = monthlyAmounts.average()
            val volatility = if (monthlyMean > 0 && monthlyAmounts.size > 1) {
                val variance = monthlyAmounts.map { (it - monthlyMean) * (it - monthlyMean) }.average()
                kotlin.math.sqrt(variance) / monthlyMean
            } else 0.0

            val recurringTotal = recurringTxns.filter { it.isActive }.sumOf { it.typicalAmount }
            val recurringRatio = if (monthlySpent > 0) recurringTotal / monthlySpent else 0.0

            val categories = allDebits.mapNotNull { it.category }.distinct().size
            val categoryDiversity = (categories.toDouble() / 10.0).coerceIn(0.0, 1.0)

            scorer.score(
                FinancialHealthScorer.FinancialMetrics(
                    savingsRate = savingsRate.coerceIn(0.0, 1.0),
                    spendingVolatility = volatility.coerceIn(0.0, 1.0),
                    recurringRatio = recurringRatio.coerceIn(0.0, 1.0),
                    categoryDiversity = categoryDiversity,
                ),
            )
        } else null

        val statisticalPredictions = StatisticalPredictions(
            spendingForecast = spendingForecast,
            spendingConfidence = spendingConfidence,
            habitSustainability = habitSustainability,
            healthTrajectory = healthTrajectory,
            stressIndex = stressIndex,
            financialHealthScore = financialHealthScore,
            lifestyleScore = null, // computed after all other scores are available
        )

        return PredictionContext(
            habitData = habitData,
            financeSummary = financeSummary,
            healthSummary = healthSummary,
            emotionalSummary = emotionalSummary,
            activitySummary = activitySummary,
            deviceStats = compactDeviceStats,
            statisticalPredictions = statisticalPredictions,
        )
    }
}
