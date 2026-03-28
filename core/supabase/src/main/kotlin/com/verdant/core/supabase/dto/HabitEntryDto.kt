package com.verdant.core.supabase.dto

import com.verdant.core.model.HabitEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class HabitEntryDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("habit_id") val habitId: String,
    val date: String,
    val completed: Boolean,
    val value: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val note: String? = null,
    val category: String? = null,
    val skipped: Boolean,
    @SerialName("missed_reason") val missedReason: String? = null,
    @SerialName("stress_level") val stressLevel: Int? = null,
    @SerialName("energy_level") val energyLevel: Int? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

fun HabitEntryDto.toDomain(): HabitEntry = HabitEntry(
    id = id,
    habitId = habitId,
    date = LocalDate.parse(date),
    completed = completed,
    value = value,
    latitude = latitude,
    longitude = longitude,
    note = note,
    category = category,
    skipped = skipped,
    missedReason = missedReason,
    stressLevel = stressLevel,
    energyLevel = energyLevel,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun HabitEntry.toDto(userId: String): HabitEntryDto = HabitEntryDto(
    id = id,
    userId = userId,
    habitId = habitId,
    date = date.toString(),
    completed = completed,
    value = value,
    latitude = latitude,
    longitude = longitude,
    note = note,
    category = category,
    skipped = skipped,
    missedReason = missedReason,
    stressLevel = stressLevel,
    energyLevel = energyLevel,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
