package com.verdant.core.ai.habit

import android.content.Context
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device habit parser backed by the ML Kit GenAI Prompt API (Gemini Nano).
 *
 * All ML Kit classes are resolved via reflection so the module compiles on devices
 * without ML Kit GenAI. Falls back to [FallbackHabitParser] when the model is
 * unavailable or inference fails.
 */
class GeminiNanoHabitParser @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val fallback: FallbackHabitParser,
) : HabitParser {

    override suspend fun parseHabitDescription(description: String): ParsedHabit =
        runCatching {
            val prompt = buildPrompt(description)
            val responseText = runInference(prompt)
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

    // -- ML Kit inference via reflection -----------------------------------------------

    private suspend fun runInference(prompt: String): String {
        val client = createClient()
            ?: throw IllegalStateException("PromptClient.create() returned null")
        return executePrompt(client, prompt)
    }

    private fun createClient(): Any? = runCatching {
        val clientClass = Class.forName("com.google.mlkit.genai.prompt.PromptClient")
        clientClass.getMethod("create", Context::class.java).invoke(null, appContext)
    }.getOrNull()

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
