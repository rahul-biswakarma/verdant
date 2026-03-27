package com.verdant.core.ai

import com.verdant.core.ai.habit.FallbackHabitParser
import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.model.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Deterministic [VerdantAI] implementation used on devices where Gemini Nano is unavailable.
 *
 * Delegates habit parsing to [FallbackHabitParser] and uses curated template strings
 * with lightweight variable substitution for motivation, nudges, and milestones.
 */
class FallbackAI @Inject constructor(
    private val habitParser: FallbackHabitParser,
) : VerdantAI {

    // ── Parsing ──────────────────────────────────────────────────────────────

    override suspend fun parseHabitDescription(text: String): ParsedHabit =
        habitParser.parseHabitDescription(text)

    override suspend fun parseBrainDump(text: String, habits: List<Habit>): ParsedBrainDump {
        val lower = text.lowercase()
        val skipKeywords = listOf("skip", "skipped", "missed", "didn't", "didnt", "couldn't", "couldnt", "no ")

        val entries = habits.mapNotNull { habit ->
            val habitLower = habit.name.lowercase()
            val nameIdx = lower.indexOf(habitLower)
            if (nameIdx == -1) {
                // Try matching any significant word from the habit name
                val words = habitLower.split(" ").filter { it.length > 3 }
                if (words.none { it in lower }) return@mapNotNull null
                words.firstOrNull { it in lower }?.let { lower.indexOf(it) } ?: return@mapNotNull null
            }
            val foundIdx = lower.indexOf(habitLower).takeIf { it != -1 }
                ?: lower.indexOf(habit.name.lowercase().split(" ").first { it.length > 3 })

            val isSkipped = skipKeywords.any { kw ->
                val kwIdx = lower.indexOf(kw)
                kwIdx != -1 && abs(kwIdx - foundIdx) < 60
            }

            // Try to extract a numeric value near the habit mention
            val valueMatch = Regex("""(\d+(?:\.\d+)?)\s*(min|minutes?|hour|hr|km|reps?|times?|x|glasses?|pages?|ml|mg|g\b)""")
                .find(lower.substring(maxOf(0, foundIdx - 10), minOf(lower.length, foundIdx + 60)))

            ParsedBrainDumpEntry(
                habitName = habit.name,
                action = if (isSkipped) BrainDumpAction.SKIPPED else BrainDumpAction.LOGGED,
                value = valueMatch?.groupValues?.get(1)?.toDoubleOrNull(),
                unit = valueMatch?.groupValues?.get(2)?.trimEnd('s'),
                skipReason = null,
            )
        }

        return ParsedBrainDump(entries = entries, unmatchedMentions = emptyList())
    }

    // ── Motivation ───────────────────────────────────────────────────────────

    override suspend fun generateMotivation(context: MotivationContext): String {
        val bestStreakEntry = context.activeStreaks.maxByOrNull { it.value }
        val bestHabit = bestStreakEntry?.let { entry ->
            context.todayHabits.firstOrNull { it.id == entry.key }
        }
        val bestStreak = bestStreakEntry?.value ?: 0
        val totalHabits = context.todayHabits.size
        val pct = (context.weekCompletion * 100).roundToInt()
        val yesterdayPct = (context.yesterdayCompletion * 100).roundToInt()

        return when {
            bestStreak >= 30 && bestHabit != null ->
                "Incredible — ${bestHabit.name} is on a $bestStreak-day streak! " +
                        "That kind of consistency is how lasting change is made."

            bestStreak >= 7 && bestHabit != null ->
                "Your ${bestHabit.name} streak is at $bestStreak days and counting. " +
                        "Keep the momentum going — you're building a habit that sticks."

            context.weekCompletion >= 0.8f && totalHabits > 0 ->
                "Brilliant week! You've completed $pct% of your habits so far. " +
                        "You're showing up consistently — that's what matters most."

            context.yesterdayCompletion >= 0.8f ->
                "Great job yesterday — $yesterdayPct% completion! " +
                        "Today is your chance to build on that streak."

            context.yesterdayCompletion < 0.4f && totalHabits > 0 ->
                "Yesterday was tough, but every day is a fresh start. " +
                        "Even completing one habit today keeps the momentum alive."

            totalHabits == 0 ->
                "Add your first habit and start building the life you want — " +
                        "small steps lead to big transformations."

            else ->
                "Consistency is built one day at a time. " +
                        "Each small action you take today is an investment in your future self."
        }
    }

    // ── Nudge ────────────────────────────────────────────────────────────────

    override suspend fun generateNudge(context: NudgeContext): String {
        val habitName = context.habit.name
        val streak = context.currentStreak
        val timeNote = context.usualCompletionTime?.let { "You usually do this around $it." }

        return when {
            streak >= 7 ->
                "Don't break your $streak-day $habitName streak! " +
                        (timeNote ?: "Just a few minutes is all it takes.")

            streak in 2..6 ->
                "You're on a $streak-day roll with $habitName. ${timeNote ?: "Keep it going!"}".trim()

            streak == 1 ->
                "Yesterday you started $habitName — keep the chain going today! " +
                        (timeNote ?: "").trim()

            else ->
                "Time to check in on $habitName. ${timeNote ?: "A small step counts!"}".trim()
        }
    }

    // ── Milestone ────────────────────────────────────────────────────────────

    override suspend fun generateMilestoneMessage(habit: Habit, milestone: Int): String {
        val name = habit.name
        return when (milestone) {
            1 -> "Day one of $name — the first step is always the hardest. You did it! 🌱"
            3 -> "3 days of $name! A small streak is forming — keep showing up."
            7 -> "One full week of $name! 🎉 You've proven you can do this."
            14 -> "Two weeks straight of $name! Your habit is starting to take root. 🌿"
            21 -> "21 days of $name — research says that's when habits start to stick. Amazing! ✨"
            30 -> "30-day milestone for $name! 🏆 A full month of commitment — you're unstoppable."
            60 -> "Two months of $name! 🔥 This habit is now a core part of who you are."
            90 -> "90 days of $name! 🌟 Three months of discipline — this is mastery in progress."
            100 -> "100 days of $name! 💯 A landmark achievement. You've built something extraordinary."
            365 -> "A full year of $name! 🎊 365 days of showing up — that's remarkable dedication."
            else -> when {
                milestone % 100 == 0 ->
                    "$milestone days of $name! 🏅 Every hundred days is a testament to your dedication."
                milestone % 30 == 0 ->
                    "${milestone / 30} months of $name! 🌳 You're growing stronger every day."
                else ->
                    "$milestone-day streak for $name! Keep up the incredible work. 🔥"
            }
        }
    }

    // ── Availability ─────────────────────────────────────────────────────────

    /** FallbackAI is always "available" — it requires no model download. */
    override fun isOnDeviceAvailable(): Flow<AIAvailability> =
        flowOf(AIAvailability.UNAVAILABLE)
}
