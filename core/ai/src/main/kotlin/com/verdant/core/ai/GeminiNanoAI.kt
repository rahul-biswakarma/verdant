package com.verdant.core.ai

import android.content.Context
import com.verdant.core.ai.habit.GeminiNanoHabitParser
import com.verdant.core.ai.habit.ParsedHabit
import com.verdant.core.model.Habit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/**
 * On-device [VerdantAI] implementation backed by the ML Kit GenAI Prompt API (Gemini Nano).
 *
 * All ML Kit classes are resolved via reflection so the module compiles cleanly on
 * devices that shipped before ML Kit GenAI was available. If any reflection call fails
 * at runtime the method automatically delegates to [FallbackAI].
 *
 * Dependency: com.google.mlkit:genai-prompt:1.0.0-beta1
 *
 * @param appContext        Android application context (Hilt @ApplicationContext).
 * @param habitParser       Existing on-device habit parser — reused to avoid duplicating
 *                          the Gemini-based parsing logic.
 * @param fallback          Deterministic fallback used when the model is unavailable or
 *                          inference fails.
 */
class GeminiNanoAI @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val habitParser: GeminiNanoHabitParser,
    private val fallback: FallbackAI,
) : VerdantAI {

    // ── Parsing ──────────────────────────────────────────────────────────────

    /** Delegates directly to [GeminiNanoHabitParser], which already handles fallback. */
    override suspend fun parseHabitDescription(text: String): ParsedHabit =
        habitParser.parseHabitDescription(text)

    override suspend fun parseBrainDump(text: String, habits: List<Habit>): List<BrainDumpResult> =
        runCatching {
            val prompt = buildBrainDumpPrompt(text, habits)
            val response = runInference(prompt)
            parseBrainDumpJson(response, habits)
        }.getOrElse { fallback.parseBrainDump(text, habits) }

    // ── Motivation ───────────────────────────────────────────────────────────

    override suspend fun generateMotivation(motivationContext: MotivationContext): String =
        runCatching {
            val prompt = buildMotivationPrompt(motivationContext)
            runInference(prompt).trim().take(200)
        }.getOrElse { fallback.generateMotivation(motivationContext) }

    // ── Nudge ────────────────────────────────────────────────────────────────

    override suspend fun generateNudge(nudgeContext: NudgeContext): String =
        runCatching {
            val prompt = buildNudgePrompt(nudgeContext)
            runInference(prompt).trim().take(140)
        }.getOrElse { fallback.generateNudge(nudgeContext) }

    // ── Milestone ────────────────────────────────────────────────────────────

    override suspend fun generateMilestoneMessage(habit: Habit, milestone: Int): String =
        runCatching {
            val prompt = buildMilestonePrompt(habit, milestone)
            runInference(prompt).trim().take(200)
        }.getOrElse { fallback.generateMilestoneMessage(habit, milestone) }

    // ── Availability flow ─────────────────────────────────────────────────────

    /**
     * Emits the current [AIAvailability] by querying the ML Kit GenAI feature status.
     *
     * The ML Kit API exposes `PromptClient.checkFeatureStatus()` which returns a
     * `Task<FeatureStatus>` whose `name()` corresponds to one of:
     * AVAILABLE, DOWNLOADING, DOWNLOADABLE, UNAVAILABLE (or NOT_SUPPORTED).
     */
    override fun isOnDeviceAvailable(): Flow<AIAvailability> = flow {
        emit(queryAvailability())
    }

    // ── Internal – brain dump helpers ─────────────────────────────────────────

    private fun buildBrainDumpPrompt(text: String, habits: List<Habit>): String {
        val habitList = habits.joinToString("\n") { h ->
            "- ID: \"${h.id}\", Name: \"${h.name}\", Type: ${h.trackingType.name}"
        }
        return """
            You are a habit tracking assistant. Given a user's activity log and their habit list, identify which habits were mentioned and what happened.

            Available habits:
            $habitList

            User log: "$text"

            Return a JSON array. For each habit recognised in the log, output one object:
            - habitId: string (must exactly match one of the IDs above)
            - action: "COMPLETE" | "SKIP" | "SET_VALUE"
            - value: number or null  (minutes for DURATION; quantity for QUANTITATIVE; amount for FINANCIAL)
            - skipReason: string or null  (short reason if action is SKIP)

            Rules:
            - Only include habits explicitly mentioned
            - BINARY / LOCATION habits → use "COMPLETE"
            - DURATION with a time → use "SET_VALUE" with minutes as value
            - QUANTITATIVE with a number → use "SET_VALUE" with that number
            - Skipped / missed / didn't / couldn't → use "SKIP"
            - Return only a valid JSON array, no markdown, no explanation
        """.trimIndent()
    }

    private fun parseBrainDumpJson(json: String, habits: List<Habit>): List<BrainDumpResult> {
        val trimmed = json.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val array = org.json.JSONArray(trimmed)
        val habitMap = habits.associateBy { it.id }
        val results = mutableListOf<BrainDumpResult>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val habit = habitMap[obj.optString("habitId")] ?: continue
            val action = runCatching {
                BrainDumpAction.valueOf(obj.optString("action", "COMPLETE"))
            }.getOrDefault(BrainDumpAction.COMPLETE)
            val value = if (obj.isNull("value")) null else obj.optDouble("value").takeIf { !it.isNaN() }
            val skipReason = obj.optString("skipReason").takeIf { it.isNotBlank() }
            results.add(BrainDumpResult(habit = habit, action = action, value = value, skipReason = skipReason))
        }
        return results
    }

    // ── Internal – prompt builders ────────────────────────────────────────────

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

    // ── Internal – ML Kit inference (reflection) ──────────────────────────────

    /**
     * Creates a PromptClient, checks availability, then runs inference.
     * Throws if anything goes wrong so callers can catch and fall back.
     */
    private suspend fun runInference(prompt: String): String {
        val client = createClient()
            ?: throw IllegalStateException("PromptClient.create() returned null")

        val available = checkClientAvailability(client)
        if (!available) throw IllegalStateException("Gemini Nano model not available")

        return executePrompt(client, prompt)
    }

    /** Reflects `PromptClient.create(Context)` → Any? */
    private fun createClient(): Any? = runCatching {
        val clientClass = Class.forName("com.google.mlkit.genai.prompt.PromptClient")
        clientClass.getMethod("create", Context::class.java).invoke(null, appContext)
    }.getOrNull()

    /** Reflects `client.isAvailable()` → Task<Boolean> */
    private suspend fun checkClientAvailability(client: Any): Boolean =
        suspendCancellableCoroutine { cont ->
            runCatching {
                val task = client.javaClass.getMethod("isAvailable").invoke(client)
                attachListeners(task,
                    onSuccess = { result -> cont.resume(result as? Boolean ?: false) },
                    onFailure = { cont.resume(false) },
                )
            }.onFailure { cont.resume(false) }
        }

    /** Reflects `PromptRequest.Builder(prompt).build()` then `client.sendPrompt(request)` */
    private suspend fun executePrompt(client: Any, prompt: String): String =
        suspendCancellableCoroutine { cont ->
            runCatching {
                val builderClass =
                    Class.forName("com.google.mlkit.genai.prompt.PromptRequest\$Builder")
                val builder = builderClass.getConstructor(String::class.java).newInstance(prompt)
                val request = builderClass.getMethod("build").invoke(builder)

                val requestClass = Class.forName("com.google.mlkit.genai.prompt.PromptRequest")
                val sendMethod = client.javaClass.getMethod("sendPrompt", requestClass)
                val task = sendMethod.invoke(client, request)

                attachListeners(task,
                    onSuccess = { result ->
                        val text = result!!.javaClass.getMethod("getText").invoke(result) as? String ?: ""
                        cont.resume(text)
                    },
                    onFailure = { e ->
                        cont.resumeWithException(e ?: RuntimeException("Inference failed"))
                    },
                )
            }.onFailure { cont.resumeWithException(it) }
        }

    /**
     * Queries availability status via `PromptClient.checkFeatureStatus()`.
     * Maps the returned enum name to [AIAvailability]. Falls back to UNAVAILABLE
     * if any reflection step fails.
     */
    private suspend fun queryAvailability(): AIAvailability {
        val client = createClient() ?: return AIAvailability.UNAVAILABLE

        return suspendCancellableCoroutine { cont ->
            runCatching {
                // Try checkFeatureStatus() first (preferred API)
                val statusMethod = runCatching {
                    client.javaClass.getMethod("checkFeatureStatus")
                }.getOrNull()

                if (statusMethod != null) {
                    val task = statusMethod.invoke(client)
                    attachListeners(task,
                        onSuccess = { status ->
                            cont.resume(mapStatusName(status?.toString()))
                        },
                        onFailure = { cont.resume(AIAvailability.UNAVAILABLE) },
                    )
                } else {
                    // Fallback: use isAvailable() boolean
                    val task = client.javaClass.getMethod("isAvailable").invoke(client)
                    attachListeners(task,
                        onSuccess = { result ->
                            val available = result as? Boolean ?: false
                            cont.resume(if (available) AIAvailability.AVAILABLE else AIAvailability.UNAVAILABLE)
                        },
                        onFailure = { cont.resume(AIAvailability.UNAVAILABLE) },
                    )
                }
            }.onFailure { cont.resume(AIAvailability.UNAVAILABLE) }
        }
    }

    /**
     * Maps the string representation of a ML Kit FeatureStatus enum value to
     * [AIAvailability]. The match is case-insensitive and substring-based to
     * remain resilient against library version changes.
     */
    private fun mapStatusName(name: String?): AIAvailability {
        val s = name?.uppercase() ?: return AIAvailability.UNAVAILABLE
        return when {
            "DOWNLOADING" in s                     -> AIAvailability.DOWNLOADING
            "DOWNLOADABLE" in s                    -> AIAvailability.DOWNLOADABLE
            "AVAILABLE" in s                       -> AIAvailability.AVAILABLE
            "NOT_SUPPORTED" in s || "UNAVAIL" in s -> AIAvailability.UNAVAILABLE
            else                                   -> AIAvailability.UNAVAILABLE
        }
    }

    // ── Shared Tasks-API listener helper ──────────────────────────────────────

    /**
     * Attaches `addOnSuccessListener` / `addOnFailureListener` to a Google Tasks
     * API [Task] object via dynamic proxy reflection.
     */
    @Suppress("UNCHECKED_CAST")
    private fun attachListeners(
        task: Any?,
        onSuccess: (Any?) -> Unit,
        onFailure: (Exception?) -> Unit,
    ) {
        if (task == null) { onFailure(null); return }

        val successInterface = Class.forName("com.google.android.gms.tasks.OnSuccessListener")
        val successProxy = java.lang.reflect.Proxy.newProxyInstance(
            successInterface.classLoader,
            arrayOf(successInterface),
        ) { _, _, args -> onSuccess(args?.firstOrNull()); null }
        task.javaClass.getMethod("addOnSuccessListener", successInterface)
            .invoke(task, successProxy)

        val failureInterface = Class.forName("com.google.android.gms.tasks.OnFailureListener")
        val failureProxy = java.lang.reflect.Proxy.newProxyInstance(
            failureInterface.classLoader,
            arrayOf(failureInterface),
        ) { _, _, args -> onFailure(args?.firstOrNull() as? Exception); null }
        task.javaClass.getMethod("addOnFailureListener", failureInterface)
            .invoke(task, failureProxy)
    }
}
