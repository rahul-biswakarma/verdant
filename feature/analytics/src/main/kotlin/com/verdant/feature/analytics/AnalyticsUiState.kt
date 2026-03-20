package com.verdant.feature.analytics

import com.verdant.core.ai.Correlation
import com.verdant.core.model.DayCell
import com.verdant.core.model.Habit

// ── Tab enum ─────────────────────────────────────────────────────────────────

enum class AnalyticsTab(val label: String) {
    OVERVIEW("Overview"),
    HEATMAPS("Heatmaps"),
    TRENDS("Trends"),
    CORRELATIONS("Correlations"),
    REPORTS("Reports"),
}

// ── Top-level UI state ────────────────────────────────────────────────────────

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val habits: List<Habit> = emptyList(),
    val selectedTab: AnalyticsTab = AnalyticsTab.OVERVIEW,
    val overview: OverviewState = OverviewState(),
    val heatmaps: HeatmapsState = HeatmapsState(),
    val trends: TrendsState = TrendsState(),
    val correlations: CorrelationsState = CorrelationsState.Idle,
    val reports: ReportsState = ReportsState.Idle,
)

// ── Overview ──────────────────────────────────────────────────────────────────

data class OverviewState(
    val completedToday: Int = 0,
    val scheduledToday: Int = 0,
    /** 0–1 completion fraction for last 7 days overall */
    val weekCompletionRate: Float = 0f,
    /** 0–1 completion fraction for last 30 days overall */
    val monthCompletionRate: Float = 0f,
    /** Top 5 streaks: habitName → streak length */
    val topStreaks: List<Pair<String, Int>> = emptyList(),
    /** Per-day completion rates for the last 7 days (oldest first) */
    val last7DaysRates: List<Float> = emptyList(),
    /** Per-day labels (e.g. "Mon", "Tue") for last 7 days */
    val last7DaysLabels: List<String> = emptyList(),
    /** Total completed entries ever */
    val totalCompletions: Int = 0,
)

// ── Heatmaps ──────────────────────────────────────────────────────────────────

data class HeatmapsState(
    val selectedHabitIndex: Int = 0,
    /** Grid cells for the selected habit, 52 weeks */
    val cells: List<DayCell> = emptyList(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val completionRate30d: Float = 0f,
)

// ── Trends ────────────────────────────────────────────────────────────────────

/**
 * One data series plotted on the trend chart.
 * @param label   Habit name, or "Overall" for the aggregate series.
 * @param color   Habit color (Long from [Habit.color]).
 * @param values  Completion rate (0–1) per week, oldest first; 12 entries.
 */
data class TrendSeries(
    val label: String,
    val color: Long,
    val values: List<Float>,
)

data class TrendsState(
    /** "overall" shows the aggregate; habit ID shows that habit's series */
    val selectedSeriesKey: String = "overall",
    val series: List<TrendSeries> = emptyList(),
    /** X-axis week labels, e.g. ["Mar 3", "Mar 10", …]; 12 entries */
    val weekLabels: List<String> = emptyList(),
)

// ── Correlations ──────────────────────────────────────────────────────────────

sealed interface CorrelationsState {
    data object Idle : CorrelationsState
    data object Loading : CorrelationsState
    data class Success(val correlations: List<Correlation>) : CorrelationsState
    data class Error(val message: String) : CorrelationsState
}

// ── Reports ───────────────────────────────────────────────────────────────────

data class ReportEntry(
    val id: String,
    val title: String,
    val content: String,
    val generatedAt: Long,
    val isWeekly: Boolean,
)

sealed interface ReportsState {
    data object Idle : ReportsState
    data object GeneratingWeekly : ReportsState
    data object GeneratingMonthly : ReportsState
    data class Ready(
        val reports: List<ReportEntry>,
        val expandedReportId: String? = null,
    ) : ReportsState

    data class Error(val message: String) : ReportsState
}
