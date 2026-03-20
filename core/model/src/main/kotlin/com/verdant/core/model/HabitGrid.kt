package com.verdant.core.model

data class HabitGrid(
    val habitId: String?,
    val cells: List<DayCell>,
    val weeks: Int,
)
