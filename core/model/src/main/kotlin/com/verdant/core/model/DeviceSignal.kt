package com.verdant.core.model

data class DeviceSignal(
    val id: String,
    val deviceId: String,
    val signalType: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
    val createdAt: Long,
)
