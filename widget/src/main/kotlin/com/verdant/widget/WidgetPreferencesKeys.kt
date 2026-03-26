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
internal object WidgetPreferencesKeys {    /** Habit being tracked (null → show data for all habits). */
    val HABIT_ID       = stringPreferencesKey("habit_id")
    val HABIT_NAME     = stringPreferencesKey("habit_name")
    val HABIT_ICON     = stringPreferencesKey("habit_icon")
    /** ARGB color packed as Long. */
    val HABIT_COLOR    = longPreferencesKey("habit_color")
    /** TrackingType.name string. */
    val TRACKING_TYPE  = stringPreferencesKey("tracking_type")
    val APP_WIDGET_ID  = intPreferencesKey("app_widget_id")
    /** Color theme: "habit" | "dark" | "light". */
    val COLOR_THEME    = stringPreferencesKey("color_theme")
    /** Grid density: "comfortable" | "compact". */
    val GRID_DENSITY   = stringPreferencesKey("grid_density")
    val STREAK         = intPreferencesKey("streak")
    /**
     * Contribution grid JSON: [{"d":"2024-01-15","i":0.75}, ...]
     * "d" = ISO date, "i" = intensity [0,1].
     */
    val GRID_JSON      = stringPreferencesKey("grid_json")
    val WEEK_DONE      = intPreferencesKey("week_done")
    val WEEK_TOTAL     = intPreferencesKey("week_total")
    val BEST_STREAK          = intPreferencesKey("best_streak")
    val COMPLETION_RATE      = floatPreferencesKey("completion_rate")
    /** Trend vs previous 30d: positive = up, negative = down, in percentage points. */
    val TREND_PCT            = floatPreferencesKey("trend_pct")    /** Total habits scheduled today for this user. */
    val TODAY_TOTAL    = intPreferencesKey("today_total")
    /** Habits completed today. */
    val TODAY_DONE     = intPreferencesKey("today_done")
    /**
     * Checklist JSON: [{"id":"...","icon":"💧","name":"Water",
     * "colorL":4287141896,"completed":true,"status":"✓ Done","binary":true}]
     */
    val CHECKLIST_JSON = stringPreferencesKey("checklist_json")    /** Absolute path to a pre-generated PNG file displayed via ImageProvider. */
    val BITMAP_PATH    = stringPreferencesKey("bitmap_path")    /**
     * Bar chart JSON: [{"label":"Mon","done":3,"total":5,"fraction":0.60,"today":false}]
     */
    val BAR_CHART_JSON = stringPreferencesKey("bar_chart_json")    /**
     * Ring habits JSON: [{"name":"Water","colorL":4287141896,"progress":0.80}]
     * Up to 5 entries rendered as concentric arcs.
     */
    val RING_JSON      = stringPreferencesKey("ring_json")    /**
     * Top streaks JSON: [{"icon":"💧","name":"Water","colorL":4287141896,"streak":15}]
     */
    val TOP_STREAKS_JSON = stringPreferencesKey("top_streaks_json")
    val BEST_EVER_NAME   = stringPreferencesKey("best_ever_name")
    val BEST_EVER_STREAK = intPreferencesKey("best_ever_streak")
    val QUOTE_TEXT   = stringPreferencesKey("quote_text")
    val QUOTE_AUTHOR = stringPreferencesKey("quote_author")
    /** ISO date the current quote was generated — used to detect daily refresh. */
    val QUOTE_DATE   = stringPreferencesKey("quote_date")

    /**
     * Weekly completion JSON for streak widget:
     * [{"day":"Mon","done":true,"partial":false}, ...]
     * 7 entries, Mon–Sun.
     */
    val WEEK_DAYS_JSON = stringPreferencesKey("week_days_json")
    /** Total all-time completions for this habit or all habits. */
    val TOTAL_COMPLETIONS = intPreferencesKey("total_completions")
    /** Best ever streak (single habit context). */
    val BEST_EVER_STREAK_SINGLE = intPreferencesKey("best_ever_streak_single")
    /** Current value for measurable habits (e.g. 6825 steps). */
    val CURRENT_VALUE  = floatPreferencesKey("current_value")
    /** Target value for measurable habits (e.g. 10000 steps). */
    val TARGET_VALUE   = floatPreferencesKey("target_value")
    /** Unit label for measurable habits (e.g. "steps", "ml", "pages"). */
    val UNIT_LABEL     = stringPreferencesKey("unit_label")
}
