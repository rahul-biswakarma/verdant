package com.verdant.core.model

enum class TrackingType {
    /** Simple yes/no completion — streaks, calisthenics, daily supplements */
    BINARY,
    /** Sequential milestones — build a drone, complete a course, assemble parts */
    CHECKPOINT,
    /** Time-based with optional intensity — coding sessions, workouts, meditation */
    DURATION,
    /** Cumulative numbers — cycling km, pages read, glasses of water */
    QUANTITATIVE,
    LOCATION,
    FINANCIAL,
    EMOTIONAL,
    EVENT_DRIVEN,
}
