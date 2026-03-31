package com.verdant.core.genui.data

import com.verdant.core.genui.model.DataSource
import com.verdant.core.genui.model.DataSourceRef
import com.verdant.core.genui.model.ResolvedData
import com.verdant.core.model.repository.EmotionalContextRepository
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.model.repository.HealthRecordRepository
import com.verdant.core.model.repository.LifeScoreRepository
import com.verdant.core.model.repository.PlayerProfileRepository
import com.verdant.core.model.repository.PredictionRepository
import com.verdant.core.model.repository.StreakCacheRepository
import com.verdant.core.model.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves [DataSourceRef] descriptors into live [ResolvedData] flows.
 * Each [DataSource] enum maps to safe, whitelisted repository queries.
 */
@Singleton
class DashboardDataResolver @Inject constructor(
    private val habitRepository: HabitRepository,
    private val habitEntryRepository: HabitEntryRepository,
    private val streakCacheRepository: StreakCacheRepository,
    private val transactionRepository: TransactionRepository,
    private val predictionRepository: PredictionRepository,
    private val emotionalContextRepository: EmotionalContextRepository,
    private val healthRecordRepository: HealthRecordRepository,
    private val lifeScoreRepository: LifeScoreRepository,
    private val playerProfileRepository: PlayerProfileRepository,
) {

    fun observeResolved(ref: DataSourceRef): Flow<ResolvedData> = when (ref.source) {
        DataSource.HABITS_TODAY -> resolveHabitsToday()
        DataSource.HABITS_COMPLETION -> resolveHabitsCompletion()
        DataSource.STREAKS -> resolveStreaks()
        DataSource.TRANSACTIONS_MONTHLY -> resolveTransactionsMonthly()
        DataSource.PREDICTIONS_ACTIVE -> resolvePredictions()
        DataSource.AI_INSIGHTS_RECENT -> flowOf(ResolvedData())
        DataSource.EMOTIONAL_LATEST -> resolveEmotionalLatest()
        DataSource.HEALTH_SUMMARY_7D -> resolveHealthSummary()
        DataSource.LIFE_SCORES -> resolveLifeScores()
        DataSource.PLAYER_PROFILE -> resolvePlayerProfile()
        DataSource.STATIC -> flowOf(ResolvedData())
        DataSource.HABIT_ENTRIES -> resolveHabitEntries(ref)
    }

    private fun resolveHabitsToday(): Flow<ResolvedData> {
        val today = LocalDate.now()
        return combine(
            habitRepository.observeActiveHabits(),
            habitEntryRepository.observeAllEntries(today, today),
        ) { habits, entries ->
            val completedIds = entries.filter { it.completed }.map { it.habitId }.toSet()
            val completed = habits.count { it.id in completedIds }
            val total = habits.size
            val topHabits = buildJsonArray {
                habits.take(5).forEach { h ->
                    add(buildJsonObject {
                        put("id", h.id)
                        put("name", h.name)
                        put("icon", h.icon)
                        put("completed", h.id in completedIds)
                    })
                }
            }
            ResolvedData(
                values = mapOf(
                    "totalHabits" to JsonPrimitive(total),
                    "completedHabits" to JsonPrimitive(completed),
                    "remainingHabits" to JsonPrimitive(total - completed),
                    "completionPercent" to JsonPrimitive(
                        if (total > 0) completed.toFloat() / total else 0f,
                    ),
                    "topHabits" to topHabits,
                ),
            )
        }
    }

    private fun resolveHabitsCompletion(): Flow<ResolvedData> {
        val today = LocalDate.now()
        return combine(
            habitRepository.observeActiveHabits(),
            habitEntryRepository.observeAllEntries(today, today),
        ) { habits, entries ->
            val completed = entries.count { it.completed }
            val total = habits.size
            val percent = if (total > 0) completed.toFloat() / total else 0f
            ResolvedData(
                values = mapOf(
                    "completionPercent" to JsonPrimitive(percent),
                    "completed" to JsonPrimitive(completed),
                    "total" to JsonPrimitive(total),
                ),
            )
        }
    }

    private fun resolveStreaks(): Flow<ResolvedData> =
        streakCacheRepository.observeAll().map { streaks ->
            val best = streaks.maxByOrNull { it.currentStreak }
            ResolvedData(
                values = mapOf(
                    "bestStreak" to JsonPrimitive(best?.currentStreak ?: 0),
                    "bestStreakHabitId" to JsonPrimitive(best?.habitId ?: ""),
                    "totalActiveStreaks" to JsonPrimitive(
                        streaks.count { it.currentStreak > 0 },
                    ),
                ),
            )
        }

    private fun resolveTransactionsMonthly(): Flow<ResolvedData> {
        val now = LocalDate.now()
        val monthStart = now.withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEnd = now.plusMonths(1).withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return combine(
            transactionRepository.totalSpent(monthStart, monthEnd),
            transactionRepository.totalIncome(monthStart, monthEnd),
        ) { spent, income ->
            ResolvedData(
                values = mapOf(
                    "monthlySpent" to JsonPrimitive(spent ?: 0.0),
                    "monthlyIncome" to JsonPrimitive(income ?: 0.0),
                ),
            )
        }
    }

    private fun resolvePredictions(): Flow<ResolvedData> {
        val now = System.currentTimeMillis()
        return predictionRepository.observeActive(now).map { predictions ->
            val items = buildJsonArray {
                predictions.forEach { p ->
                    add(buildJsonObject {
                        put("type", p.predictionType.name)
                        put("data", p.predictionData)
                        put("confidence", p.confidence)
                    })
                }
            }
            ResolvedData(
                values = mapOf(
                    "predictions" to items,
                    "count" to JsonPrimitive(predictions.size),
                ),
            )
        }
    }

    private fun resolveEmotionalLatest(): Flow<ResolvedData> =
        emotionalContextRepository.observeLatest().map { ctx ->
            if (ctx == null) return@map ResolvedData()
            ResolvedData(
                values = mapOf(
                    "mood" to JsonPrimitive(ctx.inferredMood.name),
                    "energy" to JsonPrimitive(ctx.energyLevel),
                    "confidence" to JsonPrimitive(ctx.confidence),
                ),
            )
        }

    private fun resolveHealthSummary(): Flow<ResolvedData> {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1_000L
        return healthRecordRepository.observeByRange(sevenDaysAgo, now).map { records ->
            val steps = records.filter { it.recordType.name == "STEPS" }
            val sleep = records.filter { it.recordType.name == "SLEEP" }
            ResolvedData(
                values = mapOf(
                    "avgSteps" to JsonPrimitive(
                        if (steps.isNotEmpty()) steps.map { it.value }.average() else 0.0,
                    ),
                    "avgSleepHours" to JsonPrimitive(
                        if (sleep.isNotEmpty()) sleep.map { it.value }.average() else 0.0,
                    ),
                    "recordCount" to JsonPrimitive(records.size),
                ),
            )
        }
    }

    private fun resolveLifeScores(): Flow<ResolvedData> {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - 30 * 24 * 60 * 60 * 1_000L
        return lifeScoreRepository.observeByRange(thirtyDaysAgo, now).map { scores ->
            val byDomain = buildJsonObject {
                scores.groupBy { it.scoreType.name }.forEach { (domain, domainScores) ->
                    put(domain, domainScores.lastOrNull()?.score ?: 0)
                }
            }
            ResolvedData(
                values = mapOf(
                    "scores" to byDomain,
                    "count" to JsonPrimitive(scores.size),
                ),
            )
        }
    }

    private fun resolvePlayerProfile(): Flow<ResolvedData> =
        playerProfileRepository.observe().map { profile ->
            if (profile == null) return@map ResolvedData()
            ResolvedData(
                values = mapOf(
                    "title" to JsonPrimitive(profile.title),
                    "level" to JsonPrimitive(profile.level),
                    "totalXP" to JsonPrimitive(profile.totalXP),
                    "rank" to JsonPrimitive(profile.rank.name),
                ),
            )
        }

    private fun resolveHabitEntries(ref: DataSourceRef): Flow<ResolvedData> {
        val habitId = ref.filters["habitId"] ?: return flowOf(ResolvedData())
        val days = ref.filters["days"]?.toIntOrNull() ?: 30
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong())
        return habitEntryRepository.observeEntries(habitId, startDate, today).map { entries ->
            val completedDates = buildJsonArray {
                entries.filter { it.completed }.forEach { add(JsonPrimitive(it.date.toString())) }
            }
            ResolvedData(
                values = mapOf(
                    "entries" to completedDates,
                    "totalEntries" to JsonPrimitive(entries.size),
                    "completedCount" to JsonPrimitive(entries.count { it.completed }),
                ),
            )
        }
    }
}
