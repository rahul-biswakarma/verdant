package com.verdant.core.voice

import javax.inject.Inject

class VoiceCommandParser @Inject constructor() {

    data class HabitLogCommand(
        val habitName: String,
        val value: Double?,
        val unit: String?,
    )

    /**
     * Parses recognized speech into habit log commands.
     * Supports patterns like:
     *  - "done with reading" / "finished meditation"
     *  - "ran 5 kilometers" / "drank 8 glasses of water"
     *  - "30 minutes yoga" / "studied for 2 hours"
     *  - "logged reading and meditation"
     */
    fun parse(text: String): List<HabitLogCommand> {
        val lower = text.lowercase().trim()

        // Split on "and" / commas for multi-habit entries
        val segments = lower.split(Regex("\\band\\b|,")).map { it.trim() }.filter { it.isNotBlank() }

        return segments.mapNotNull { parseSegment(it) }
    }

    private fun parseSegment(segment: String): HabitLogCommand? {
        // Pattern 1: "done with X" / "finished X" / "completed X"
        val donePattern = Regex("(?:done\\s+with|finished|completed|did)\\s+(.+)")
        donePattern.find(segment)?.let {
            return HabitLogCommand(habitName = it.groupValues[1].trim(), value = null, unit = null)
        }

        // Pattern 2: "VALUE UNIT of HABIT" (e.g. "8 glasses of water")
        val valueOfPattern = Regex("(\\d+\\.?\\d*)\\s+(\\w+)\\s+of\\s+(.+)")
        valueOfPattern.find(segment)?.let {
            return HabitLogCommand(
                habitName = it.groupValues[3].trim(),
                value = it.groupValues[1].toDoubleOrNull(),
                unit = it.groupValues[2],
            )
        }

        // Pattern 3: "HABIT for VALUE UNIT" (e.g. "studied for 2 hours")
        val habitForValue = Regex("(.+?)\\s+for\\s+(\\d+\\.?\\d*)\\s+(\\w+)")
        habitForValue.find(segment)?.let {
            return HabitLogCommand(
                habitName = it.groupValues[1].trim(),
                value = it.groupValues[2].toDoubleOrNull(),
                unit = it.groupValues[3],
            )
        }

        // Pattern 4: "VALUE UNIT HABIT" (e.g. "30 minutes yoga", "5 km run")
        val valueUnitHabit = Regex("(\\d+\\.?\\d*)\\s+(\\w+)\\s+(.+)")
        valueUnitHabit.find(segment)?.let {
            val unit = it.groupValues[2]
            if (unit in KNOWN_UNITS) {
                return HabitLogCommand(
                    habitName = it.groupValues[3].trim(),
                    value = it.groupValues[1].toDoubleOrNull(),
                    unit = unit,
                )
            }
        }

        // Pattern 5: "HABIT VALUE UNIT" (e.g. "ran 5 kilometers")
        val habitValueUnit = Regex("(\\w+)\\s+(\\d+\\.?\\d*)\\s+(\\w+)")
        habitValueUnit.find(segment)?.let {
            return HabitLogCommand(
                habitName = it.groupValues[1].trim(),
                value = it.groupValues[2].toDoubleOrNull(),
                unit = it.groupValues[3],
            )
        }

        // Fallback: treat entire segment as habit name (binary completion)
        val cleaned = segment
            .replace(Regex("^(log|track|mark|check off)\\s+"), "")
            .trim()
        if (cleaned.isNotBlank()) {
            return HabitLogCommand(habitName = cleaned, value = null, unit = null)
        }

        return null
    }

    companion object {
        private val KNOWN_UNITS = setOf(
            "minutes", "minute", "min", "mins",
            "hours", "hour", "hr", "hrs",
            "km", "kilometers", "kilometres", "miles", "mi",
            "glasses", "cups", "liters", "litres", "ml",
            "pages", "chapters",
            "reps", "sets",
            "steps",
        )
    }
}
