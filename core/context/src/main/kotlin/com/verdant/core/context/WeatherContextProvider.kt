package com.verdant.core.context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class WeatherContext(
    val isPrecipitationLikely: Boolean,
    val temperatureCelsius: Float,
    val condition: String, // "clear", "cloudy", "rain", "snow"
)

@Singleton
class WeatherContextProvider @Inject constructor() {

    private var cachedWeather: WeatherContext? = null
    private var cacheTimestamp: Long = 0L
    private val cacheMutex = Mutex()

    companion object {
        private const val CACHE_DURATION_MS = 3 * 60 * 60 * 1000L // 3 hours
    }

    /**
     * Fetches weather from the Open-Meteo API (free, no key needed).
     * Results are cached in memory for 3 hours.
     * Returns null if the request fails or location is unavailable.
     */
    suspend fun getWeather(lat: Double = 0.0, lon: Double = 0.0): WeatherContext? {
        cacheMutex.withLock {
            val now = System.currentTimeMillis()
            if (cachedWeather != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
                return cachedWeather
            }
        }

        if (lat == 0.0 && lon == 0.0) return null

        val result = fetchFromApi(lat, lon) ?: return null

        cacheMutex.withLock {
            cachedWeather = result
            cacheTimestamp = System.currentTimeMillis()
        }

        return result
    }

    private suspend fun fetchFromApi(lat: Double, lon: Double): WeatherContext? =
        withContext(Dispatchers.IO) {
            try {
                val urlString = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,precipitation,weather_code"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                try {
                    if (connection.responseCode != 200) return@withContext null

                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val current = json.getJSONObject("current")

                    val temperature = current.getDouble("temperature_2m").toFloat()
                    val precipitation = current.getDouble("precipitation").toFloat()
                    val weatherCode = current.getInt("weather_code")

                    val condition = mapWeatherCode(weatherCode)
                    val isPrecipitationLikely = precipitation > 0.1f ||
                        weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)

                    WeatherContext(
                        isPrecipitationLikely = isPrecipitationLikely,
                        temperatureCelsius = temperature,
                        condition = condition,
                    )
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Maps WMO weather codes to simple condition strings.
     * See: https://open-meteo.com/en/docs#weathervariables
     */
    private fun mapWeatherCode(code: Int): String = when (code) {
        0 -> "clear"
        1, 2 -> "clear"       // mainly clear, partly cloudy
        3 -> "cloudy"          // overcast
        45, 48 -> "cloudy"     // fog
        51, 53, 55 -> "rain"   // drizzle
        56, 57 -> "rain"       // freezing drizzle
        61, 63, 65 -> "rain"   // rain
        66, 67 -> "rain"       // freezing rain
        71, 73, 75 -> "snow"   // snowfall
        77 -> "snow"           // snow grains
        80, 81, 82 -> "rain"   // rain showers
        85, 86 -> "snow"       // snow showers
        95 -> "rain"           // thunderstorm
        96, 99 -> "rain"       // thunderstorm with hail
        else -> "clear"
    }
}
