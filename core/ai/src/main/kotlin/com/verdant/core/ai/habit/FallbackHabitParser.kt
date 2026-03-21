package com.verdant.core.ai.habit

import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.TrackingType
import javax.inject.Inject

/**
 * Keyword-based habit parser used when Gemini Nano is unavailable.
 *
 * Matches common habit keywords to pre-configured templates, with a
 * sensible generic default for unrecognized descriptions.
 */
class FallbackHabitParser @Inject constructor() : HabitParser {

    override suspend fun parseHabitDescription(description: String): ParsedHabit {
        val lower = description.lowercase()
        val base = RULES.firstOrNull { rule -> rule.keywords.any { lower.contains(it) } }
            ?.toHabit(description)
            ?: genericHabit(description)

        // Detect multiple time-of-day keywords and override reminder times
        val detectedTimes = detectTimeOfDay(lower)
        return if (detectedTimes.isNotEmpty()) {
            // Extract non-time context as description (e.g., "before food")
            val extraContext = extractContext(lower)
            val desc = if (extraContext.isNotBlank() && base.description != extraContext) {
                "$extraContext${if (base.description.isNullOrBlank()) "" else " — ${base.description}"}"
            } else {
                base.description
            }
            base.copy(
                suggestedReminderTime = detectedTimes.joinToString(","),
                description = desc?.take(120),
            )
        } else {
            base
        }
    }

    /** Detect time-of-day keywords and map to HH:mm times. */
    private fun detectTimeOfDay(lower: String): List<String> {
        val times = mutableListOf<String>()
        if (lower.contains("morning") || lower.contains("sunrise") || lower.contains("wake up")) {
            times.add("07:00")
        }
        if (lower.contains("afternoon") || lower.contains("lunch")) {
            times.add("13:00")
        }
        if (lower.contains("evening") || lower.contains("dinner") || lower.contains("sunset")) {
            times.add("19:00")
        }
        if (lower.contains("night") || lower.contains("bedtime") || lower.contains("before sleep")) {
            times.add("21:00")
        }
        return times
    }

    /** Extract context phrases like "before food", "after workout", etc. */
    private fun extractContext(lower: String): String {
        val contextPatterns = listOf("before food", "after food", "before meal", "after meal",
            "before workout", "after workout", "before breakfast", "after breakfast",
            "before lunch", "after lunch", "before dinner", "after dinner",
            "on empty stomach", "with food", "with water")
        return contextPatterns.filter { lower.contains(it) }.joinToString(", ")
            .replaceFirstChar { it.uppercase() }
    }

    private data class Rule(
        val keywords: List<String>,
        val name: String,
        val icon: String,
        val color: Long,
        val label: String,
        val trackingType: TrackingType,
        val unit: String?,
        val targetValue: Double?,
        val reminderTime: String?,
    ) {
        fun toHabit(rawDescription: String) = ParsedHabit(
            name = name,
            icon = icon,
            color = color,
            label = label,
            trackingType = trackingType,
            unit = unit,
            targetValue = targetValue,
            frequency = HabitFrequency.DAILY,
            scheduleDays = 0x7F, // all days
            suggestedReminderTime = reminderTime,
            description = rawDescription.take(120),
        )
    }

    private fun genericHabit(description: String) = ParsedHabit(
        name = description.split(" ").take(4).joinToString(" ")
            .replaceFirstChar { it.uppercase() }.take(40),
        icon = "🌱",
        color = 0xFF5A7A60L,
        label = "Lifestyle",
        trackingType = TrackingType.BINARY,
        unit = null,
        targetValue = null,
        frequency = HabitFrequency.DAILY,
        scheduleDays = 0x7F,
        suggestedReminderTime = "08:00",
        description = description.take(120),
    )

    private companion object {
        // scheduleDays bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
        private const val WEEKDAYS = 0x1F   // 31
        private const val ALL_DAYS = 0x7F   // 127

        val RULES = listOf(
            // ── Health ──────────────────────────────────────────────────────
            Rule(
                keywords = listOf("vitamin", "supplement", "pill"),
                name = "Take vitamins", icon = "💊", color = 0xFF5A7A60L,
                label = "Health", trackingType = TrackingType.BINARY,
                unit = null, targetValue = null, reminderTime = "08:00",
            ),
            Rule(
                keywords = listOf("water", "hydrat", "drink"),
                name = "Drink water", icon = "💧", color = 0xFF2196F3L,
                label = "Health", trackingType = TrackingType.QUANTITATIVE,
                unit = "glasses", targetValue = 8.0, reminderTime = "09:00",
            ),
            Rule(
                keywords = listOf("meditat", "mindful", "breathe", "breathing"),
                name = "Meditate", icon = "🧘", color = 0xFF9C27B0L,
                label = "Health", trackingType = TrackingType.DURATION,
                unit = "min", targetValue = 10.0, reminderTime = "07:00",
            ),
            Rule(
                keywords = listOf("sleep", "bed", "bedtime", "11pm", "10pm", "early"),
                name = "Sleep early", icon = "😴", color = 0xFF3F51B5L,
                label = "Health", trackingType = TrackingType.BINARY,
                unit = null, targetValue = null, reminderTime = "22:00",
            ),
            Rule(
                keywords = listOf("junk", "fast food", "snack", "sugar", "soda"),
                name = "No junk food", icon = "🥗", color = 0xFF4CAF50L,
                label = "Health", trackingType = TrackingType.BINARY,
                unit = null, targetValue = null, reminderTime = null,
            ),
            // ── Fitness ─────────────────────────────────────────────────────
            Rule(
                keywords = listOf("cycl", "bike", "bicycle"),
                name = "Cycling", icon = "🚴", color = 0xFFFF9800L,
                label = "Fitness", trackingType = TrackingType.DURATION,
                unit = "min", targetValue = 30.0, reminderTime = "07:00",
            ),
            Rule(
                keywords = listOf("run", "jog", "jogging", "sprint"),
                name = "Running", icon = "🏃", color = 0xFFFF5722L,
                label = "Fitness", trackingType = TrackingType.QUANTITATIVE,
                unit = "km", targetValue = 5.0, reminderTime = "06:30",
            ),
            Rule(
                keywords = listOf("walk", "step", "10k", "10,000"),
                name = "Walk 10k steps", icon = "👟", color = 0xFF8BC34AL,
                label = "Fitness", trackingType = TrackingType.QUANTITATIVE,
                unit = "steps", targetValue = 10000.0, reminderTime = null,
            ),
            Rule(
                keywords = listOf("gym", "workout", "lift", "weight", "strength"),
                name = "Gym workout", icon = "🏋️", color = 0xFFE91E63L,
                label = "Fitness", trackingType = TrackingType.DURATION,
                unit = "min", targetValue = 60.0, reminderTime = "07:00",
            ),
            // ── Learning ────────────────────────────────────────────────────
            Rule(
                keywords = listOf("read", "book", "page"),
                name = "Read books", icon = "📚", color = 0xFF00BCD4L,
                label = "Learning", trackingType = TrackingType.QUANTITATIVE,
                unit = "pages", targetValue = 20.0, reminderTime = "21:00",
            ),
            Rule(
                keywords = listOf("cod", "program", "develop", "algorithm"),
                name = "Practice coding", icon = "💻", color = 0xFF607D8BL,
                label = "Learning", trackingType = TrackingType.DURATION,
                unit = "min", targetValue = 30.0, reminderTime = "20:00",
            ),
            Rule(
                keywords = listOf("language", "vocab", "spanish", "french", "german", "duolingo", "japanese", "chinese"),
                name = "Learn language", icon = "🗣️", color = 0xFF009688L,
                label = "Learning", trackingType = TrackingType.DURATION,
                unit = "min", targetValue = 15.0, reminderTime = "19:00",
            ),
            // ── Finance ─────────────────────────────────────────────────────
            Rule(
                keywords = listOf("spend", "budget", "expense", "track money"),
                name = "Track spending", icon = "💰", color = 0xFFFFEB3BL,
                label = "Finance", trackingType = TrackingType.FINANCIAL,
                unit = null, targetValue = null, reminderTime = "20:00",
            ),
            Rule(
                keywords = listOf("impulse", "no buy", "shopping", "purchase"),
                name = "No impulse buying", icon = "🛒", color = 0xFFFF9800L,
                label = "Finance", trackingType = TrackingType.BINARY,
                unit = null, targetValue = null, reminderTime = null,
            ),
            Rule(
                keywords = listOf("save", "saving", "invest"),
                name = "Save money", icon = "🏦", color = 0xFF4CAF50L,
                label = "Finance", trackingType = TrackingType.FINANCIAL,
                unit = null, targetValue = null, reminderTime = "09:00",
            ),
            // ── Lifestyle ───────────────────────────────────────────────────
            Rule(
                keywords = listOf("journal", "diary", "write", "writing"),
                name = "Journal", icon = "📓", color = 0xFF795548L,
                label = "Lifestyle", trackingType = TrackingType.BINARY,
                unit = null, targetValue = null, reminderTime = "21:30",
            ),
            Rule(
                keywords = listOf("social media", "instagram", "tiktok", "twitter", "scroll", "phone"),
                name = "No social media", icon = "📵", color = 0xFF607D8BL,
                label = "Lifestyle", trackingType = TrackingType.BINARY,
                unit = null, targetValue = null, reminderTime = null,
            ),
            Rule(
                keywords = listOf("cook", "meal prep", "home food", "homemade"),
                name = "Cook at home", icon = "🍳", color = 0xFFFF8F00L,
                label = "Lifestyle", trackingType = TrackingType.BINARY,
                unit = null, targetValue = null, reminderTime = "17:30",
            ),
        )
    }
}
