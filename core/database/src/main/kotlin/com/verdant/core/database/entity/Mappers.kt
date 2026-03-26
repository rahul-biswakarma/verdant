package com.verdant.core.database.entity

import com.verdant.core.model.Habit
import com.verdant.core.model.HabitEntry
import com.verdant.core.model.Label

// ── HabitEntity ↔ Habit ───────────────────────────────────────────────────────

fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    label = label,
    trackingType = trackingType,
    unit = unit,
    targetValue = targetValue,
    frequency = frequency,
    scheduleDays = scheduleDays,
    isArchived = isArchived,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime,
    reminderDays = reminderDays,
    sortOrder = sortOrder,
    createdAt = createdAt,
)

fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    label = label,
    trackingType = trackingType,
    unit = unit,
    targetValue = targetValue,
    frequency = frequency,
    scheduleDays = scheduleDays,
    isArchived = isArchived,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime,
    reminderDays = reminderDays,
    sortOrder = sortOrder,
    createdAt = createdAt,
)

// ── HabitEntryEntity ↔ HabitEntry ────────────────────────────────────────────

fun HabitEntryEntity.toDomain() = HabitEntry(
    id = id,
    habitId = habitId,
    date = date,
    completed = completed,
    value = value,
    latitude = latitude,
    longitude = longitude,
    note = note,
    category = category,
    skipped = skipped,
    createdAt = createdAt,
    updatedAt = updatedAt,
    missedReason = missedReason,
    stressLevel = stressLevel,
    energyLevel = energyLevel,
)

fun HabitEntry.toEntity() = HabitEntryEntity(
    id = id,
    habitId = habitId,
    date = date,
    completed = completed,
    value = value,
    latitude = latitude,
    longitude = longitude,
    note = note,
    category = category,
    skipped = skipped,
    createdAt = createdAt,
    updatedAt = updatedAt,
    missedReason = missedReason,
    stressLevel = stressLevel,
    energyLevel = energyLevel,
)

// ── LabelEntity ↔ Label ───────────────────────────────────────────────────────

fun LabelEntity.toDomain() = Label(id = id, name = name, color = color)

fun Label.toEntity() = LabelEntity(id = id, name = name, color = color)
