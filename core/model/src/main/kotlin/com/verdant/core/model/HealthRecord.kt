package com.verdant.core.model

data class HealthRecord(
    val id: String,
    val recordType: HealthRecordType,
    val value: Double,
    val secondaryValue: Double?,
    val unit: String,
    val recordedAt: Long,
    val source: String,
    val createdAt: Long,
)

enum class HealthRecordType {
    STEPS,
    SLEEP,
    HEART_RATE,
    WEIGHT,
    EXERCISE,
    HYDRATION,
}
