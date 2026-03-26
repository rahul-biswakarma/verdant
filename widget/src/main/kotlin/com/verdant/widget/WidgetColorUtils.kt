package com.verdant.widget

import androidx.compose.ui.graphics.Color

/** Empty-cell color in the grid (matches a dark neutral). */
internal val CellEmptyColor = Color(0xFF2C2C2E)

/**
 * Maps [intensity] ∈ [0,1] to a visual color derived from [baseColor].
 *
 * | intensity | meaning                  |
 * |-----------|--------------------------|
 * | 0.00      | no entry — dark gray     |
 * | 0.01–0.25 | faint tint  (level 1)   |
 * | 0.26–0.50 | medium tint (level 2)   |
 * | 0.51–0.75 | strong tint (level 3)   |
 * | 0.76–1.00 | full base color (level 4)|
 *
 * Binary habits only produce 0.0 or 1.0, so they get level 0 or level 4.
 */
fun intensityColor(baseColor: Color, intensity: Float): Color = when {
    intensity <= 0f    -> CellEmptyColor
    intensity <= 0.25f -> baseColor.copy(alpha = 0.28f)
    intensity <= 0.50f -> baseColor.copy(alpha = 0.52f)
    intensity <= 0.75f -> baseColor.copy(alpha = 0.76f)
    else               -> baseColor
}
/**
 * Serialise a (date → intensity) map to the compact JSON the widget reads.
 * Format: [{"d":"2024-01-15","i":0.75}, ...]
 */
internal fun buildGridJson(cells: List<Pair<String, Float>>): String {
    val array = org.json.JSONArray()
    for ((date, intensity) in cells) {
        array.put(org.json.JSONObject().apply {
            put("d", date)
            put("i", intensity.toDouble())
        })
    }
    return array.toString()
}

/**
 * Deserialise the grid JSON back to a (ISO date string → intensity) map
 * for consumption inside the Glance composable.
 */
internal fun parseGridJson(json: String): Map<String, Float> {
    if (json.isBlank() || json == "[]") return emptyMap()
    return try {
        val array = org.json.JSONArray(json)
        buildMap(array.length()) {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                put(obj.getString("d"), obj.getDouble("i").toFloat())
            }
        }
    } catch (_: Exception) {
        emptyMap()
    }
}
