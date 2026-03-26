package com.verdant.core.model

import java.time.LocalDate

/**
 * A single day's mood entry for the Year-in-Pixels grid.
 *
 * @param date      The calendar day.
 * @param moodScore Mood rating 1–5 (1 = terrible, 5 = great).
 * @param note      Optional journal text logged with the mood.
 */
data class PixelEntry(
    val date: LocalDate,
    val moodScore: Int,
    val note: String? = null,
)

/** Converts a [HabitEntry] from an EMOTIONAL habit into a [PixelEntry], or null if invalid. */
fun HabitEntry.toPixelEntry(): PixelEntry? {
    val score = value?.toInt()?.takeIf { it in 1..5 } ?: return null
    return PixelEntry(date = date, moodScore = score, note = note)
}
