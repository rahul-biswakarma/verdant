package com.verdant.core.model

enum class InsightType {
    DAILY_MOTIVATION,
    PATTERN_RECOGNITION,
    STREAK_ALERT,
    CORRELATION,
    WEEKLY_SUMMARY,
    MONTHLY_SUMMARY,
    SUGGESTION,
    /** A Fogg habit-stack formula pairing a consistent anchor habit with a struggling target habit. */
    HABIT_STACK,
    /** Cross-domain weekly synthesis — correlations between habits and contextual signals (stress, energy). */
    BEHAVIORAL_SYNTHESIS,
    SPENDING_FORECAST,
    HEALTH_INSIGHT,
    STRESS_ALERT,
    LIFE_FORECAST,
    CROSS_CORRELATION,
    BUDGET_ALERT,
    ANOMALY,
}
