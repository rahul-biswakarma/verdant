package com.verdant.core.context

import com.verdant.core.model.WeatherCondition
import javax.inject.Inject

class WeatherContextProvider @Inject constructor() {

    data class WeatherContext(
        val temperature: Double,
        val condition: WeatherCondition,
        val isPrecipitationLikely: Boolean,
    )

    suspend fun getWeather(lat: Double, lon: Double): WeatherContext? {
        // TODO: Lightweight weather check via Open-Meteo (free, no API key)
        return null
    }
}
