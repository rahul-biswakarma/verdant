package com.verdant.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Preference keys stored per widget instance in Glance's DataStore.
 * Keys are namespaced per-instance automatically by PreferencesGlanceStateDefinition.
 *
 * Naming convention: keys shared across widget types use a generic name;
 * type-specific keys are prefixed by widget type abbreviation.
 */
internal object WidgetPreferencesKeys {

    // ── Shared / per-habit identity ──────────────────────────────────────────
    /** Habit being tracked (null → show data for all habits). */
    val HABIT_ID       = stringPreferencesKey("habit_id")
    val HABIT_NAME     = stringPreferencesKey("habit_name")
    val HABIT_ICON     = stringPreferencesKey("habit_icon")
    /** ARGB color packed as Long. */
    val HABIT_COLOR    = longPreferencesKey("habit_color")
    /** TrackingType.name string. */
    val TRACKING_TYPE  = stringPreferencesKey("tracking_type")
    val APP_WIDGET_ID  = intPreferencesKey("app_widget_id")

    // ── User preferences ─────────────────────────────────────────────────────
    /** Color theme: "habit" | "dark" | "light". */
    val COLOR_THEME    = stringPreferencesKey("color_theme")
    /** Grid density: "comfortable" | "compact". */
    val GRID_DENSITY   = stringPreferencesKey("grid_density")

    // ── HabitGridWidget / MiniHeatmapWidget ──────────────────────────────────
    val STREAK         = intPreferencesKey("streak")
    /**
     * Contribution grid JSON: [{"d":"2024-01-15","i":0.75}, ...]
     * "d" = ISO date, "i" = intensity [0,1].
     */
    val GRID_JSON      = stringPreferencesKey("grid_json")
    val WEEK_DONE      = intPreferencesKey("week_done")
    val WEEK_TOTAL     = intPreferencesKey("week_total")

    // ── HabitGridWidget (extra stats) ──────────────────────────────────────────
    val TOTAL_COMPLETIONS    = intPreferencesKey("total_completions")
    val BEST_EVER_STREAK_SINGLE = intPreferencesKey("best_ever_streak_single")

    // ── ProgressWidget ─────────────────────────────────────────────────────────
    val PROGRESS_VALUE       = floatPreferencesKey("progress_value")
    val PROGRESS_TARGET      = floatPreferencesKey("progress_target")
    val PROGRESS_UNIT        = stringPreferencesKey("progress_unit")

    // ── StreakWidget (per-habit measurable) ─────────────────────────────────────
    val WEEK_DAYS_JSON       = stringPreferencesKey("week_days_json")
    val UNIT_LABEL           = stringPreferencesKey("unit_label")
    val CURRENT_VALUE        = floatPreferencesKey("current_value")
    val TARGET_VALUE         = floatPreferencesKey("target_value")

    // ── MiniHeatmapWidget (extra stats) ──────────────────────────────────────
    val BEST_STREAK          = intPreferencesKey("best_streak")
    val COMPLETION_RATE      = floatPreferencesKey("completion_rate")
    /** Trend vs previous 30d: positive = up, negative = down, in percentage points. */
    val TREND_PCT            = floatPreferencesKey("trend_pct")

    // ── ChecklistWidget / SummaryWidget ──────────────────────────────────────
    /** Total habits scheduled today for this user. */
    val TODAY_TOTAL    = intPreferencesKey("today_total")
    /** Habits completed today. */
    val TODAY_DONE     = intPreferencesKey("today_done")
    /**
     * Checklist JSON: [{"id":"...","icon":"💧","name":"Water",
     * "colorL":4287141896,"completed":true,"status":"✓ Done","binary":true}]
     */
    val CHECKLIST_JSON = stringPreferencesKey("checklist_json")

    // ── SummaryWidget / RadialRingWidget / QuoteWidget (Bitmap-backed) ───────
    /** Absolute path to a pre-generated PNG file displayed via ImageProvider. */
    val BITMAP_PATH    = stringPreferencesKey("bitmap_path")

    // ── BarChartWidget ────────────────────────────────────────────────────────
    /**
     * Bar chart JSON: [{"label":"Mon","done":3,"total":5,"fraction":0.60,"today":false}]
     */
    val BAR_CHART_JSON = stringPreferencesKey("bar_chart_json")

    // ── RadialRingWidget ──────────────────────────────────────────────────────
    /**
     * Ring habits JSON: [{"name":"Water","colorL":4287141896,"progress":0.80}]
     * Up to 5 entries rendered as concentric arcs.
     */
    val RING_JSON      = stringPreferencesKey("ring_json")

    // ── StreakWidget ──────────────────────────────────────────────────────────
    /**
     * Top streaks JSON: [{"icon":"💧","name":"Water","colorL":4287141896,"streak":15}]
     */
    val TOP_STREAKS_JSON = stringPreferencesKey("top_streaks_json")
    val BEST_EVER_NAME   = stringPreferencesKey("best_ever_name")
    val BEST_EVER_STREAK = intPreferencesKey("best_ever_streak")

    // ── QuoteWidget ───────────────────────────────────────────────────────────
    val QUOTE_TEXT   = stringPreferencesKey("quote_text")
    val QUOTE_AUTHOR = stringPreferencesKey("quote_author")
    /** ISO date the current quote was generated — used to detect daily refresh. */
    val QUOTE_DATE   = stringPreferencesKey("quote_date")

    // ── QuickToggleWidget ─────────────────────────────────────────────────────
    /** Whether the habit has been completed today. */
    val QUICK_TOGGLE_COMPLETED = booleanPreferencesKey("quick_toggle_completed")

    // ── TimerWidget ───────────────────────────────────────────────────────────
    /** System.currentTimeMillis() when the timer was started; 0 = stopped. */
    val TIMER_START_MS      = longPreferencesKey("timer_start_ms")
    /** Accumulated seconds from previous (completed) timer sessions today. */
    val TIMER_TOTAL_SECONDS = intPreferencesKey("timer_total_seconds")

    // ── MultiHabitWidget ──────────────────────────────────────────────────────
    /** Comma-separated list of habit IDs selected for this widget instance. */
    val MULTI_HABIT_IDS  = stringPreferencesKey("multi_habit_ids")
    /**
     * Multi-habit checklist JSON (same schema as CHECKLIST_JSON) but only for
     * the configured habits: [{"id":"...","icon":"🏃","name":"Run","colorL":...,"completed":true,...}]
     */
    val MULTI_HABIT_JSON = stringPreferencesKey("multi_habit_json")
}
