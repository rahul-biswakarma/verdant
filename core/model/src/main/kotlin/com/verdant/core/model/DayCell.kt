package com.verdant.core.model

import java.time.LocalDate

data class DayCell(
    val date: LocalDate,
    /** Completion intensity in [0, 1]. */
    val intensity: Float,
    val entryCount: Int,
    val completedCount: Int,
    val isSkipped: Boolean = false,
)
