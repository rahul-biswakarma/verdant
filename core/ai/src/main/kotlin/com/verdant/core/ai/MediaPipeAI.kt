package com.verdant.core.ai

import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.ai.mediapipe.ModelDownloadState
import com.verdant.core.ai.mediapipe.ModelManager
import com.verdant.core.model.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * On-device [VerdantAI] implementation backed by MediaPipe LLM Inference
 * running Gemma 2B Q4.
 *
 * Replaces the previous Gemini Nano (ML Kit) implementation with a direct
 * MediaPipe API — no reflection needed, works on any device with 6 GB+ RAM.
 *
 * Falls back to [FallbackAI] when the model is not downloaded or inference fails.
 */
@Singleton
class MediaPipeAI @Inject constructor(
    private val modelManager: ModelManager,
    private val habitParser: com.verdant.core.ai.habit.MediaPipeHabitParser,
    private val fallback: FallbackAI,
) : VerdantAI {

    override suspend fun parseHabitDescription(text: String): ParsedHabit =
        habitParser.parseHabitDescription(text)

    override suspend fun parseBrainDump(text: String, habits: List<Habit>): ParsedBrainDump =
        fallback.parseBrainDump(text, habits)

    override suspend fun generateMotivation(motivationContext: MotivationContext): String =
        runCatching {
            val prompt = buildMotivationPrompt(motivationContext)
            modelManager.runInference(prompt).trim().take(200)
        }.getOrElse { fallback.generateMotivation(motivationContext) }

    override suspend fun generateNudge(nudgeContext: NudgeContext): String =
        runCatching {
            val prompt = buildNudgePrompt(nudgeContext)
            modelManager.runInference(prompt).trim().take(140)
        }.getOrElse { fallback.generateNudge(nudgeContext) }

    override suspend fun generateMilestoneMessage(habit: Habit, milestone: Int): String =
        runCatching {
            val prompt = buildMilestonePrompt(habit, milestone)
            modelManager.runInference(prompt).trim().take(200)
        }.getOrElse { fallback.generateMilestoneMessage(habit, milestone) }

    override fun isOnDeviceAvailable(): Flow<AIAvailability> =
        modelManager.state.map { state ->
            when (state) {
                is ModelDownloadState.Ready -> AIAvailability.AVAILABLE
                is ModelDownloadState.Loading -> AIAvailability.DOWNLOADING
                is ModelDownloadState.Downloaded -> AIAvailability.AVAILABLE
                is ModelDownloadState.Downloading -> AIAvailability.DOWNLOADING
                is ModelDownloadState.NotDownloaded -> AIAvailability.DOWNLOADABLE
                is ModelDownloadState.Error -> AIAvailability.UNAVAILABLE
                is ModelDownloadState.Unsupported -> AIAvailability.UNAVAILABLE
            }
        }


    private fun buildMotivationPrompt(ctx: MotivationContext): String {
        val topStreak = ctx.activeStreaks.values.maxOrNull() ?: 0
        val topHabitName = ctx.activeStreaks
            .maxByOrNull { it.value }
            ?.key
            ?.let { id -> ctx.todayHabits.firstOrNull { it.id == id }?.name }
            ?: ctx.todayHabits.firstOrNull()?.name ?: "your habits"
        val totalHabits = ctx.todayHabits.size
        val weekPct = (ctx.weekCompletion * 100).roundToInt()
        val yesterdayPct = (ctx.yesterdayCompletion * 100).roundToInt()

        return """
            You are a supportive habit coach. Write exactly 1-2 short, warm sentences of motivation.
            Do not use bullet points, markdown, or quotes. Respond only with the motivational text.

            User stats:
            - Habits today: $totalHabits
            - Best active streak: $topStreak days ("$topHabitName")
            - Yesterday completion: $yesterdayPct%
            - This week completion: $weekPct%
        """.trimIndent()
    }

    private fun buildNudgePrompt(ctx: NudgeContext): String {
        val timeHint = ctx.usualCompletionTime
            ?.let { "The user usually does this around $it." }
            ?: "No usual time recorded."

        return """
            You are a friendly habit tracker. Write exactly 1 short sentence nudging the user to complete their habit now.
            Do not use bullet points, markdown, or exclamation spam. Respond only with the nudge.

            Habit: "${ctx.habit.name}"
            Current streak: ${ctx.currentStreak} days
            Current time: ${ctx.currentTime}
            $timeHint
        """.trimIndent()
    }

    private fun buildMilestonePrompt(habit: Habit, milestone: Int): String = """
        You are a supportive habit coach. Write exactly 1-2 upbeat sentences celebrating a milestone achievement.
        Include an appropriate emoji at the end. Do not use markdown or quotes. Respond only with the celebration text.

        Habit: "${habit.name}"
        Milestone: $milestone consecutive days completed
    """.trimIndent()
}
