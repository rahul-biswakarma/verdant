package com.verdant.core.ai.habit

import com.verdant.core.ai.mediapipe.ModelManager
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import org.json.JSONObject
import javax.inject.Inject

/**
 * On-device habit parser using MediaPipe LLM Inference (Gemma 2B).
 *
 * Falls back to [FallbackHabitParser] when the model is unavailable or
 * inference fails.
 */
class MediaPipeHabitParser @Inject constructor(
    private val modelManager: ModelManager,
    private val fallback: FallbackHabitParser,
) : HabitParser {

    override suspend fun parseHabitDescription(description: String): ParsedHabit =
        runCatching {
            val prompt = buildPrompt(description)
            val responseText = modelManager.runInference(prompt)
            parseJsonResponse(responseText, description)
        }.getOrElse { fallback.parseHabitDescription(description) }

    private fun buildPrompt(description: String): String = """
        Parse the following habit description and return a JSON object with these fields:
        - name: string (short, 2-4 words)
        - icon: string (single emoji)
        - color: string (hex color like #5A7A60)
        - label: string (one of: Health, Fitness, Learning, Finance, Lifestyle)
        - trackingType: string (one of: BINARY, NUMERIC) — use NUMERIC for any habit involving a number, count, duration, or money
        - unit: string or null
        - targetValue: number or null
        - frequency: string (one of: DAILY, WEEKDAYS, WEEKENDS, SPECIFIC_DAYS)
        - scheduleDays: number (bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64; all=127)
        - suggestedReminderTime: string (comma-separated HH:mm times, e.g. "07:00,19:00" for morning and evening) or null
        - description: string (one sentence)

        Habit description: "$description"

        Return only valid JSON, no markdown or explanation.
    """.trimIndent()

    private fun parseJsonResponse(json: String, originalDescription: String): ParsedHabit {
        val trimmed = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(trimmed)

        val colorHex = obj.optString("color", "#5A7A60").trimStart('#')
        val colorLong = colorHex.toLongOrNull(16)?.let { 0xFF000000L or it } ?: 0xFF5A7A60L

        return ParsedHabit(
            name = obj.optString("name", originalDescription.take(30)),
            icon = obj.optString("icon", "\uD83C\uDF31"),
            color = colorLong,
            label = obj.optString("label", "Lifestyle"),
            trackingType = run {
                val raw = obj.optString("trackingType", "BINARY")
                when (raw) {
                    "QUANTITATIVE", "DURATION", "FINANCIAL" -> TrackingType.QUANTITATIVE
                    else -> runCatching { TrackingType.valueOf(raw) }.getOrDefault(TrackingType.BINARY)
                }
            },
            unit = obj.optString("unit").takeIf { it.isNotBlank() },
            targetValue = if (obj.isNull("targetValue")) null else obj.optDouble("targetValue"),
            frequency = runCatching {
                HabitFrequency.valueOf(obj.optString("frequency", "DAILY"))
            }.getOrDefault(HabitFrequency.DAILY),
            scheduleDays = obj.optInt("scheduleDays", 0x7F),
            suggestedReminderTime = obj.optString("suggestedReminderTime").takeIf { it.isNotBlank() },
            description = obj.optString("description").takeIf { it.isNotBlank() },
        )
    }
}
