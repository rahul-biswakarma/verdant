package com.verdant.core.model

import java.time.LocalDate

data class HabitEntry(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val completed: Boolean,
    val value: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val note: String?,
    val category: String?,
    val skipped: Boolean,
    val missedReason: String?,
    val stressLevel: Int?,
    val energyLevel: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val missedReason: String? = null,
    val stressLevel: Int? = null,
    val energyLevel: Int? = null,
)
