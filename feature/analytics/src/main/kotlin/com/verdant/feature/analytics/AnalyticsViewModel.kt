package com.verdant.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.ai.AIFeatureUnavailableException
import com.verdant.core.ai.CorrelationData
import com.verdant.core.ai.MonthlyReportData
import com.verdant.core.ai.VerdantAI
import com.verdant.core.ai.WeeklyReportData
import com.verdant.core.common.HabitDataAggregator
import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.usecase.CalculateStreakUseCase
import com.verdant.core.model.DayCell
import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.InsightType
import com.verdant.core.model.isScheduledForDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val entryRepository: HabitEntryRepository,
    private val calculateStreak: CalculateStreakUseCase,
    private val insightDao: AIInsightDao,
    private val verdantAI: VerdantAI,
    private val aggregator: HabitDataAggregator,
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = _state

    private val monthFormatter = DateTimeFormatter.ofPattern("MMM d")

    init {
        viewModelScope.launch {
            habitRepository.observeActiveHabits().collect { habits ->
                if (habits.isEmpty()) {
                    _state.update { it.copy(isLoading = false, habits = emptyList()) }
                    return@collect
                }
                _state.update { it.copy(habits = habits) }
                reloadData(habits)
            }
        }
        loadReports()
    }


    fun selectTab(tab: AnalyticsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }


    fun selectHabitForHeatmap(index: Int) {
        val habits = _state.value.habits
        if (index !in habits.indices) return
        viewModelScope.launch {
            val habit = habits[index]
            _state.update { it.copy(heatmaps = it.heatmaps.copy(selectedHabitIndex = index)) }
            loadHeatmapForHabit(habit, index)
        }
    }


    fun selectTrendSeries(key: String) {
        _state.update { it.copy(trends = it.trends.copy(selectedSeriesKey = key)) }
    }


    fun generateCorrelations() {
        val habits = _state.value.habits
        if (habits.isEmpty()) return
        _state.update { it.copy(correlations = CorrelationsState.Loading) }
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now()
                val start = today.minusDays(29)
                val entries = entryRepository.observeAllEntries(start, today).first()
                val streaks = calculateStreak.currentStreaks(habits.map { it.id })
                val aggregated = aggregator.aggregateForDailyInsight(habits, entries, streaks, today, 30)

                verdantAI.findCorrelations(
                    CorrelationData(habits = habits, aggregatedData = aggregated, periodDays = 30)
                )
            }.fold(
                onSuccess = { correlations ->
                    _state.update { it.copy(correlations = CorrelationsState.Success(correlations)) }
                },
                onFailure = { e ->
                    val msg = when (e) {
                        is AIFeatureUnavailableException -> when (e.reason) {
                            AIFeatureUnavailableException.Reason.NO_NETWORK ->
                                "Connect to the internet to find correlations"
                            AIFeatureUnavailableException.Reason.RATE_LIMITED ->
                                "Daily AI limit reached — try again tomorrow"
                            else -> "Could not generate correlations"
                        }
                        else -> "Could not generate correlations"
                    }
                    _state.update { it.copy(correlations = CorrelationsState.Error(msg)) }
                }
            )
        }
    }


    fun generateWeeklyReport() {
        val habits = _state.value.habits
        if (habits.isEmpty()) return
        _state.update { it.copy(reports = ReportsState.GeneratingWeekly) }
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now()
                val weekStart = today.with(DayOfWeek.MONDAY).minusWeeks(1)
                val weekEnd = weekStart.plusDays(6)
                val entries = entryRepository.observeAllEntries(weekStart, weekEnd).first()
                val streaks = calculateStreak.currentStreaks(habits.map { it.id })
                val aggregated = aggregator.aggregateForReport(habits, entries, streaks, today, 7)

                verdantAI.generateWeeklyReport(
                    WeeklyReportData(weekStart, weekEnd, aggregated, habits)
                )
            }.fold(
                onSuccess = { report ->
                    val content = buildString {
                        appendLine(report.summary)
                        if (report.highlights.isNotEmpty()) {
                            appendLine("\n✨ Highlights")
                            report.highlights.forEach { appendLine("• $it") }
                        }
                        if (report.patterns.isNotEmpty()) {
                            appendLine("\n📊 Patterns")
                            report.patterns.forEach { appendLine("• $it") }
                        }
                        if (report.suggestions.isNotEmpty()) {
                            appendLine("\n💡 Suggestions")
                            report.suggestions.forEach { appendLine("• $it") }
                        }
                    }
                    loadReports(pendingEntry = ReportEntry(
                        id = "weekly_${System.currentTimeMillis()}",
                        title = "Weekly Report — ${monthFormatter.format(LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1))}",
                        content = content,
                        generatedAt = System.currentTimeMillis(),
                        isWeekly = true,
                    ))
                },
                onFailure = { e ->
                    val msg = aiErrorMessage(e)
                    _state.update { it.copy(reports = ReportsState.Error(msg)) }
                }
            )
        }
    }

    fun generateMonthlyReport() {
        val habits = _state.value.habits
        if (habits.isEmpty()) return
        _state.update { it.copy(reports = ReportsState.GeneratingMonthly) }
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now()
                val monthStart = today.withDayOfMonth(1).minusMonths(1)
                val monthEnd = monthStart.plusMonths(1).minusDays(1)
                val entries = entryRepository.observeAllEntries(monthStart, monthEnd).first()
                val streaks = calculateStreak.currentStreaks(habits.map { it.id })
                val aggregated = aggregator.aggregateForReport(habits, entries, streaks, today, 30)

                verdantAI.generateMonthlyReport(
                    MonthlyReportData(monthStart, monthEnd, aggregated, habits)
                )
            }.fold(
                onSuccess = { report ->
                    val today = LocalDate.now()
                    val content = buildString {
                        appendLine(report.summary)
                        if (report.highlights.isNotEmpty()) {
                            appendLine("\n✨ Highlights")
                            report.highlights.forEach { appendLine("• $it") }
                        }
                        if (report.patterns.isNotEmpty()) {
                            appendLine("\n📊 Patterns")
                            report.patterns.forEach { appendLine("• $it") }
                        }
                        if (report.suggestions.isNotEmpty()) {
                            appendLine("\n💡 Suggestions")
                            report.suggestions.forEach { appendLine("• $it") }
                        }
                    }
                    loadReports(pendingEntry = ReportEntry(
                        id = "monthly_${System.currentTimeMillis()}",
                        title = "Monthly Report — ${today.minusMonths(1).month.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                        content = content,
                        generatedAt = System.currentTimeMillis(),
                        isWeekly = false,
                    ))
                },
                onFailure = { e ->
                    val msg = aiErrorMessage(e)
                    _state.update { it.copy(reports = ReportsState.Error(msg)) }
                }
            )
        }
    }

    fun toggleReportExpanded(id: String) {
        val current = _state.value.reports
        if (current is ReportsState.Ready) {
            val newExpanded = if (current.expandedReportId == id) null else id
            _state.update { it.copy(reports = current.copy(expandedReportId = newExpanded)) }
        }
    }


    private suspend fun reloadData(habits: List<Habit>) {
        val today = LocalDate.now()
        val start52w = today.minusDays(365)

        val allEntries = entryRepository.observeAllEntries(start52w, today).first()
        val streakMap  = calculateStreak.currentStreaks(habits.map { it.id })

        loadOverview(habits, allEntries, streakMap, today)
        loadHeatmapForHabit(habits.first(), 0, allEntries)
        loadTrends(habits, allEntries, today)

        _state.update { it.copy(isLoading = false) }
    }

    private fun loadOverview(
        habits: List<Habit>,
        allEntries: List<HabitEntry>,
        streakMap: Map<String, Int>,
        today: LocalDate,
    ) {
        val todayBit = 1 shl (today.dayOfWeek.value - 1)
        val scheduledToday = habits.count { it.scheduleDays and todayBit != 0 }
        val completedToday = allEntries.count { it.date == today && it.completed }

        // 7-day completion rate
        val weekRates = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val scheduled = habits.filter { it.isScheduledForDate(date) }
            if (scheduled.isEmpty()) return@map 0f
            val done = allEntries.count { it.date == date && it.completed }
            done.toFloat() / scheduled.size
        }

        // 30-day overall
        val monthRate = run {
            var total = 0; var done = 0
            for (offset in 0 until 30) {
                val date = today.minusDays(offset.toLong())
                val sched = habits.filter { it.isScheduledForDate(date) }
                total += sched.size
                done += allEntries.count { it.date == date && it.completed }
            }
            if (total == 0) 0f else done.toFloat() / total
        }

        val topStreaks = streakMap.entries
            .sortedByDescending { it.value }
            .take(3)
            .mapNotNull { entry ->
                val h = habits.find { it.id == entry.key } ?: return@mapNotNull null
                h.name to entry.value
            }

        val dayLabels = (6 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }

        val totalCompletions = allEntries.count { it.completed }

        _state.update { s ->
            s.copy(
                overview = OverviewState(
                    completedToday    = completedToday,
                    scheduledToday    = scheduledToday,
                    weekCompletionRate = weekRates.average().toFloat(),
                    monthCompletionRate = monthRate,
                    topStreaks        = topStreaks,
                    last7DaysRates    = weekRates,
                    last7DaysLabels   = dayLabels,
                    totalCompletions  = totalCompletions,
                )
            )
        }
    }

    private suspend fun loadHeatmapForHabit(
        habit: Habit,
        index: Int,
        preloadedEntries: List<HabitEntry>? = null,
    ) {
        val today = LocalDate.now()
        val start52w = today.minusDays(365)

        val entries = preloadedEntries
            ?.filter { it.habitId == habit.id }
            ?: entryRepository.observeEntries(habit.id, start52w, today).first()

        val entryByDate = entries.associateBy { it.date }

        // Build 52-week grid cells
        val cells = (364 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val entry = entryByDate[date]
            val intensity = when {
                entry == null -> 0f
                entry.completed -> {
                    val entryValue = entry.value
                    val targetValue = habit.targetValue
                    if (entryValue != null && targetValue != null) {
                        (entryValue / targetValue).toFloat().coerceIn(0f, 1f)
                    } else 1f
                }
                entry.skipped -> 0.15f
                else -> 0f
            }
            DayCell(date = date, intensity = intensity, entryCount = 1, completedCount = if (entry?.completed == true) 1 else 0)
        }

        val currentStreak  = calculateStreak.currentStreak(habit.id)
        val longestStreak  = calculateStreak.longestStreak(habit.id)
        val totalDone      = entries.count { it.completed }
        val rate30d        = calculateStreak.completionRate(habit.id, 30)

        _state.update { s ->
            s.copy(
                heatmaps = HeatmapsState(
                    selectedHabitIndex = index,
                    cells              = cells,
                    currentStreak      = currentStreak,
                    longestStreak      = longestStreak,
                    totalCompletions   = totalDone,
                    completionRate30d  = rate30d,
                )
            )
        }
    }

    private fun loadTrends(
        habits: List<Habit>,
        allEntries: List<HabitEntry>,
        today: LocalDate,
    ) {
        val series = mutableListOf<TrendSeries>()

        // 1. Overall Completion (last 30 days)
        val overallPoints = (29 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val scheduled = habits.filter { it.isScheduledForDate(date) }
            val completed = allEntries.count { it.date == date && it.completed }
            if (scheduled.isEmpty()) 0f else completed.toFloat() / scheduled.size
        }
        series.add(TrendSeries("Overall Completion", 0xFF808080, overallPoints))

        // 2. Numeric habits (e.g. "Water", "Steps")
        habits.filter { it.targetValue != null }.forEach { habit ->
            val points = (29 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                val entry = allEntries.find { it.habitId == habit.id && it.date == date }
                entry?.value?.toFloat() ?: 0f
            }
            series.add(TrendSeries(habit.name, habit.color, points))
        }

        _state.update { it.copy(trends = TrendsState(series = series)) }
    }

    private fun aiErrorMessage(e: Throwable): String {
        return when (e) {
            is AIFeatureUnavailableException -> when (e.reason) {
                AIFeatureUnavailableException.Reason.NO_NETWORK -> "Network connection required"
                AIFeatureUnavailableException.Reason.RATE_LIMITED -> "AI limit reached"
                AIFeatureUnavailableException.Reason.NOT_SUPPORTED -> "GenAI not supported on this device"
                else -> "AI service unavailable"
            }
            else -> "Failed to generate report"
        }
    }

    private fun loadReports(pendingEntry: ReportEntry? = null) {
        viewModelScope.launch {
            insightDao.observeRecent(50).collect { entities ->
                val reports = entities.filter {
                    it.type == InsightType.WEEKLY_SUMMARY || it.type == InsightType.MONTHLY_SUMMARY
                }.map { entity ->
                    ReportEntry(
                        id = entity.id,
                        title = if (entity.type == InsightType.WEEKLY_SUMMARY) "Weekly Report" else "Monthly Report",
                        content = entity.content,
                        generatedAt = entity.generatedAt,
                        isWeekly = entity.type == InsightType.WEEKLY_SUMMARY
                    )
                }
                val all = (if (pendingEntry != null) listOf(pendingEntry) else emptyList()) + reports
                _state.update { it.copy(reports = ReportsState.Ready(all.sortedByDescending { r -> r.generatedAt })) }
            }
        }
    }
}
