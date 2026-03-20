package com.verdant.core.model

import java.time.LocalDate

data class Streak(
    val habitId: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val length: Int,
    val isActive: Boolean,
)
