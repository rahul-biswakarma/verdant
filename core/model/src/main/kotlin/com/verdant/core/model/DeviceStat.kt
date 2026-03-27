package com.verdant.core.model

data class DeviceStat(
    val id: String,
    val statType: DeviceStatType,
    val value: Double,
    val detail: String?,
    val recordedDate: Long,
    val createdAt: Long,
)

enum class DeviceStatType {
    SCREEN_TIME,
    APP_USAGE,
    NOTIFICATION_COUNT,
    BATTERY_DRAIN,
    CALENDAR_BUSY_HOURS,
}
