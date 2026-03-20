package com.verdant.widget

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
}
