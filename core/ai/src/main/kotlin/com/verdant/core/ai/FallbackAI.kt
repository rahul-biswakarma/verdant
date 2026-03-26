package com.verdant.core.ai

import com.verdant.core.ai.habit.FallbackHabitParser
import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.model.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
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
    override suspend fun parseHabitDescription(text: String): ParsedHabit =
        habitParser.parseHabitDescription(text)
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
                "${bestHabit.name} has been going for $bestStreak days — " +
                        "that kind of consistency adds up in ways that aren't always visible yet."

            bestStreak >= 7 && bestHabit != null ->
                "${bestHabit.name} is at $bestStreak days. " +
                        "Showing up repeatedly is exactly how habits take root."

            context.weekCompletion >= 0.8f && totalHabits > 0 ->
                "You've completed $pct% of your habits this week — " +
                        "that's a genuinely solid foundation to build from."

            context.yesterdayCompletion >= 0.8f ->
                "Yesterday landed at $yesterdayPct% — a good day. " +
                        "Today is simply the next opportunity to keep moving."

            context.yesterdayCompletion < 0.4f && totalHabits > 0 ->
                "Yesterday had its challenges — that's completely normal. " +
                        "One small step today is all it takes to stay in motion."

            totalHabits == 0 ->
                "Whenever you're ready, adding your first habit is a meaningful starting point — " +
                        "even small, regular actions compound over time."

            else ->
                "Progress rarely looks dramatic day-to-day. " +
                        "What matters is that you're here and showing up."
        }
    }
    override suspend fun generateNudge(context: NudgeContext): String {
        val habitName = context.habit.name
        val streak = context.currentStreak
        val timeNote = context.usualCompletionTime?.let { "You usually get to this around $it." }

        return when {
            streak >= 7 ->
                "$habitName has been part of your last $streak days — " +
                        (timeNote ?: "even a few minutes counts today.")

            streak in 2..6 ->
                "A $streak-day run with $habitName. ${timeNote ?: "How are you feeling about it today?"}".trim()

            streak == 1 ->
                "You made a start with $habitName yesterday. " +
                        (timeNote ?: "Ready to build on that?").trim()

            else ->
                "Whenever feels right — $habitName is waiting. ${timeNote ?: "A small step is still a step."}".trim()
        }
    }
    override suspend fun generateMilestoneMessage(habit: Habit, milestone: Int): String {
        val name = habit.name
        return when (milestone) {
            1 -> "Day one of $name — starting is genuinely the hardest part. You're in motion. 🌱"
            3 -> "Three days of $name. A small pattern is beginning to form. 🌿"
            7 -> "One week of $name. Seven consecutive choices add up to something real. 🎉"
            14 -> "Two weeks with $name. The habit is finding its place in your routine. 🌿"
            21 -> "21 days of $name — the research on habit formation is in your corner now. ✨"
            30 -> "A full month of $name. That's genuine consistency, not just intention. 🏆"
            60 -> "Two months of $name. This has become part of how you live. 🔥"
            90 -> "90 days of $name. Three months of showing up — that's a real practice. 🌟"
            100 -> "100 days of $name. A hundred small decisions compounded into something meaningful. 💯"
            365 -> "A year of $name. 365 days of choosing this — that's a quiet kind of extraordinary. 🎊"
            else -> when {
                milestone % 100 == 0 ->
                    "$milestone days of $name. Every hundred is worth pausing to notice. 🏅"
                milestone % 30 == 0 ->
                    "${milestone / 30} months of $name. Steady and real. 🌳"
                else ->
                    "$milestone days of $name — the consistency is building. 🔥"
            }
        }
    }
    /** FallbackAI is always "available" — it requires no model download. */
    override fun isOnDeviceAvailable(): Flow<AIAvailability> =
        flowOf(AIAvailability.UNAVAILABLE)
}
