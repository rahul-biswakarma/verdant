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
 * On-device habit parser using Gemini Nano via the ML Kit GenAI Prompt API.
 *
 * Falls back to [FallbackHabitParser] automatically when the device does not
 * support Gemini Nano or when model inference fails.
 *
 * API reference: com.google.mlkit:genai-prompt:1.0.0-beta1
 * (com.google.mlkit.genai.prompt package)
 */
class GeminiNanoHabitParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fallback: FallbackHabitParser,
) : HabitParser {

    override suspend fun parseHabitDescription(description: String): ParsedHabit =
        runCatching { parseWithGemini(description) }
            .getOrElse { fallback.parseHabitDescription(description) }

    private suspend fun parseWithGemini(description: String): ParsedHabit {
        val client = createPromptClient() ?: return fallback.parseHabitDescription(description)

        val isAvailable = checkAvailability(client)
        if (!isAvailable) return fallback.parseHabitDescription(description)

        val prompt = buildPrompt(description)
        val responseText = runInference(client, prompt)

        return parseJsonResponse(responseText, description)
    }

    /**
     * Creates the ML Kit GenAI PromptClient instance.
     * Returns null if the library is unavailable at runtime.
     */
    private fun createPromptClient(): Any? = runCatching {
        val clientClass = Class.forName("com.google.mlkit.genai.prompt.PromptClient")
        val createMethod = clientClass.getMethod("create", Context::class.java)
        createMethod.invoke(null, context)
    }.getOrNull()

    /**
     * Checks if Gemini Nano is available on this device.
     * Uses the Tasks API, bridged to coroutines via suspendCancellableCoroutine.
     */
    private suspend fun checkAvailability(client: Any): Boolean =
        suspendCancellableCoroutine { cont ->
            runCatching {
                val isAvailableMethod = client.javaClass.getMethod("isAvailable")
                val task = isAvailableMethod.invoke(client)
                addTaskListeners(task,
                    onSuccess = { result -> cont.resume(result as? Boolean ?: false) },
                    onFailure = { cont.resume(false) },
                )
            }.onFailure { cont.resume(false) }
        }

    /**
     * Runs inference with the given prompt string.
     * Bridges the Tasks API to coroutines.
     */
    private suspend fun runInference(client: Any, prompt: String): String =
        suspendCancellableCoroutine { cont ->
            runCatching {
                // Build PromptRequest via reflection (API: PromptRequest.Builder(text).build())
                val requestBuilderClass =
                    Class.forName("com.google.mlkit.genai.prompt.PromptRequest\$Builder")
                val builder = requestBuilderClass.getConstructor(String::class.java).newInstance(prompt)
                val buildMethod = requestBuilderClass.getMethod("build")
                val request = buildMethod.invoke(builder)

                // Call client.sendPrompt(request) → Task<PromptResponse>
                val sendPromptMethod = client.javaClass.getMethod(
                    "sendPrompt",
                    Class.forName("com.google.mlkit.genai.prompt.PromptRequest"),
                )
                val task = sendPromptMethod.invoke(client, request)

                addTaskListeners(task,
                    onSuccess = { result ->
                        // PromptResponse.getText() → String
                        val getTextMethod = result!!.javaClass.getMethod("getText")
                        cont.resume(getTextMethod.invoke(result) as? String ?: "")
                    },
                    onFailure = { e -> cont.resumeWithException(e ?: RuntimeException("Inference failed")) },
                )
            }.onFailure { cont.resumeWithException(it) }
        }

    /** Attaches addOnSuccessListener / addOnFailureListener to a Tasks API Task via reflection. */
    @Suppress("UNCHECKED_CAST")
    private fun addTaskListeners(
        task: Any?,
        onSuccess: (Any?) -> Unit,
        onFailure: (Exception?) -> Unit,
    ) {
        if (task == null) { onFailure(null); return }
        val taskClass = task.javaClass

        // addOnSuccessListener
        val successListenerClass = Class.forName("com.google.android.gms.tasks.OnSuccessListener")
        val successProxy = java.lang.reflect.Proxy.newProxyInstance(
            successListenerClass.classLoader,
            arrayOf(successListenerClass),
        ) { _, _, args -> onSuccess(args?.firstOrNull()); null }
        taskClass.getMethod("addOnSuccessListener", successListenerClass)
            .invoke(task, successProxy)

        // addOnFailureListener
        val failureListenerClass = Class.forName("com.google.android.gms.tasks.OnFailureListener")
        val failureProxy = java.lang.reflect.Proxy.newProxyInstance(
            failureListenerClass.classLoader,
            arrayOf(failureListenerClass),
        ) { _, _, args -> onFailure(args?.firstOrNull() as? Exception); null }
        taskClass.getMethod("addOnFailureListener", failureListenerClass)
            .invoke(task, failureProxy)
    }

    private fun buildPrompt(description: String): String = """
        Parse the following habit description and return a JSON object with these fields:
        - name: string (short, 2-4 words)
        - icon: string (single emoji)
        - color: string (hex color like #5A7A60)
        - label: string (one of: Health, Fitness, Learning, Finance, Lifestyle)
        - trackingType: string (one of: BINARY, QUANTITATIVE, DURATION, FINANCIAL)
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
            icon = obj.optString("icon", "🌱"),
            color = colorLong,
            label = obj.optString("label", "Lifestyle"),
            trackingType = runCatching {
                TrackingType.valueOf(obj.optString("trackingType", "BINARY"))
            }.getOrDefault(TrackingType.BINARY),
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
