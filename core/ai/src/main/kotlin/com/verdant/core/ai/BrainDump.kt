package com.verdant.core.ai

enum class BrainDumpAction { LOGGED, SKIPPED }

/**
 * A single habit entry parsed from a brain-dump text.
 *
 * @param habitName  Name as stated/inferred from the user's text (matches a known habit).
 * @param action     Whether the habit was done or explicitly skipped.
 * @param value      Numeric value if provided (e.g. "20 min" → 20.0).
 * @param unit       Unit string if provided (e.g. "min", "km", "reps").
 * @param skipReason Brief reason if action == SKIPPED (e.g. "tired", "no time").
 */
data class ParsedBrainDumpEntry(
    val habitName: String,
    val action: BrainDumpAction,
    val value: Double?,
    val unit: String?,
    val skipReason: String?,
)

/**
 * Result of parsing a multi-habit brain dump text.
 *
 * @param entries            Habit entries that were matched to the user's habit list.
 * @param unmatchedMentions  Things the AI detected but couldn't map to any known habit.
 */
data class ParsedBrainDump(
    val entries: List<ParsedBrainDumpEntry>,
    val unmatchedMentions: List<String>,
)
