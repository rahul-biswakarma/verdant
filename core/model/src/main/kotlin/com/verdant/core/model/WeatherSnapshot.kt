package com.verdant.core.model

data class WeatherSnapshot(
    val id: String,
    val date: Long,
    val temperature: Double,
    val condition: WeatherCondition,
    val humidity: Int,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
)

enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    HEAVY_RAIN,
    SNOW,
    THUNDERSTORM,
    FOG,
    WINDY,
}
