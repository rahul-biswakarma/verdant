package com.verdant.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.ai.MotivationContext
import com.verdant.core.ai.VerdantAI
import com.verdant.core.database.dao.TransactionDao
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.model.AlertType
import com.verdant.core.model.DashboardAlert
import com.verdant.core.model.DashboardHighlight
import com.verdant.core.model.FinanceDashboardCard
import com.verdant.core.model.HabitsDashboardCard
import com.verdant.core.model.VerdantProduct
import com.verdant.core.model.isScheduledForDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val greeting: String = "",
    val formattedDate: String = "",
    val aiInsight: String? = null,
    val habitsSummary: HabitsDashboardCard? = null,
    val financeSummary: FinanceDashboardCard? = null,
    val alerts: List<DashboardAlert> = emptyList(),
    val highlights: List<DashboardHighlight> = emptyList(),
    val enabledProducts: Set<VerdantProduct> = setOf(VerdantProduct.HABITS),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val transactionDao: TransactionDao,
    private val prefs: UserPreferencesDataStore,
    private val verdantAI: VerdantAI,
) : ViewModel() {

    private val today = LocalDate.now()

    private val _streaks = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _aiInsight = MutableStateFlow<String?>(null)
    private val _financeSummary = MutableStateFlow<FinanceDashboardCard?>(null)

    val uiState: StateFlow<HomeUiState> = com.verdant.core.common.combine(
        habitRepository.observeActiveHabits(),
        entryRepository.observeAllEntries(today, today),
        _streaks,
        _aiInsight,
        _financeSummary,
        prefs.financeOnboardingCompleted,
    ) { habits, entries, streaks, aiInsight, financeSummary, financeOnboarded ->
        val todayHabits = habits.filter { it.isScheduledForDate(today) }
        val entryMap = entries.associateBy { it.habitId }
        val completed = todayHabits.count { habit -> entryMap[habit.id]?.completed == true }

        // Build alerts
        val alerts = mutableListOf<DashboardAlert>()
        val highlights = mutableListOf<DashboardHighlight>()

        // At-risk streaks (streak >= 3, not completed today)
        todayHabits.forEach { habit ->
            val streak = streaks[habit.id] ?: 0
            val done = entryMap[habit.id]?.completed == true
            if (streak >= 3 && !done) {
                alerts.add(
                    DashboardAlert(
                        product = VerdantProduct.HABITS,
                        type = AlertType.AT_RISK,
                        title = "${habit.name} streak at risk",
                        description = "$streak-day streak — don't break it!",
                    )
                )
            }
        }

        // Streak highlights
        val topStreak = streaks.maxByOrNull { it.value }
        if (topStreak != null && topStreak.value >= 7) {
            val habitName = habits.find { it.id == topStreak.key }?.name ?: "Habit"
            highlights.add(
                DashboardHighlight(
                    product = VerdantProduct.HABITS,
                    text = "${topStreak.value}-day $habitName streak!",
                )
            )
        }

        val enabledProducts = mutableSetOf(VerdantProduct.HABITS)
        if (financeOnboarded) enabledProducts.add(VerdantProduct.FINANCE)

        HomeUiState(
            greeting = greeting(),
            formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            aiInsight = aiInsight,
            habitsSummary = HabitsDashboardCard(
                completedCount = completed,
                totalCount = todayHabits.size,
                topStreak = topStreak?.let { entry ->
                    val name = habits.find { it.id == entry.key }?.name ?: "Habit"
                    name to entry.value
                },
                atRiskHabits = alerts
                    .filter { it.product == VerdantProduct.HABITS && it.type == AlertType.AT_RISK }
                    .map { it.title },
            ),
            financeSummary = financeSummary,
            alerts = alerts,
            highlights = highlights,
            enabledProducts = enabledProducts,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        loadStreaks()
        loadAiInsight()
        loadFinanceSummary()
    }

    private fun loadStreaks() {
        viewModelScope.launch {
            habitRepository.observeActiveHabits().collect { list ->
                val todayHabits = list.filter { it.isScheduledForDate(today) }
                val newStreaks = calculateStreakUseCase.currentStreaks(todayHabits.map { it.id })
                _streaks.value = newStreaks
            }
        }
    }

    private fun loadAiInsight() {
        viewModelScope.launch {
            try {
                val habits = habitRepository.getAllHabits()
                val todayHabits = habits.filter { !it.isArchived && it.isScheduledForDate(today) }
                if (todayHabits.isEmpty()) return@launch

                val ctx = MotivationContext(
                    todayHabits = todayHabits,
                    activeStreaks = _streaks.value,
                    yesterdayCompletion = 0f,
                    weekCompletion = 0f,
                )
                val insight = verdantAI.generateMotivation(ctx)
                if (insight.isNotBlank()) _aiInsight.value = insight
            } catch (_: Exception) { }
        }
    }

    private fun loadFinanceSummary() {
        viewModelScope.launch {
            try {
                val zone = ZoneId.systemDefault()
                val month = YearMonth.now()
                val startMs = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val endMs = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

                transactionDao.totalSpent(startMs, endMs).collect { spent ->
                    _financeSummary.value = FinanceDashboardCard(
                        monthlySpent = spent ?: 0.0,
                        topCategory = null,
                        lastTransactionAge = null,
                    )
                }
            } catch (_: Exception) { }
        }
    }
}

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
