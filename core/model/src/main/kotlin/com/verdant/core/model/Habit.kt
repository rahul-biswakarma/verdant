package com.verdant.core.model

data class Habit(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val color: Long,
    val label: String?,
    val trackingType: TrackingType,
    val visualizationType: VisualizationType,
    val unit: String?,
    val targetValue: Double?,
    /** Ordered milestone steps for CHECKPOINT habits; empty for other types. */
    val checkpointSteps: List<String>,
    val frequency: HabitFrequency,
    /** Bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64 */
    val scheduleDays: Int,
    val isArchived: Boolean,
    val reminderEnabled: Boolean,
    val reminderTime: String?,
    val reminderDays: Int,
    val visualizationType: VisualizationType = VisualizationType.CONTRIBUTION_GRID,
    val sortOrder: Int,
    val createdAt: Long,
)
