package com.verdant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val color: Long,
    val label: String?,
    @ColumnInfo(name = "tracking_type") val trackingType: TrackingType,
    val unit: String?,
    @ColumnInfo(name = "target_value") val targetValue: Double?,
    val frequency: HabitFrequency,
    @ColumnInfo(name = "schedule_days") val scheduleDays: Int,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean,
    @ColumnInfo(name = "reminder_enabled") val reminderEnabled: Boolean,
    @ColumnInfo(name = "reminder_time") val reminderTime: String?,
    @ColumnInfo(name = "reminder_days") val reminderDays: Int,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
