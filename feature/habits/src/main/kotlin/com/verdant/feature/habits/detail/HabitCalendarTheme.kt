package com.verdant.feature.habits.detail

import androidx.compose.ui.graphics.Color

/**
 * A curated color palette for the habit calendar view.
 * Each theme is hand-crafted for visual cohesion rather than derived algorithmically.
 */
data class HabitCalendarTheme(
    val name: String,
    val headerBg: Color,
    val headerGradientEnd: Color,
    val headerText: Color,
    val headerSubtle: Color,
    val completedDayBg: Color,
    val completedDayText: Color,
    val emptyDayBorder: Color,
    val emptyDayText: Color,
    val skippedBorder: Color,
    val skippedText: Color,
    val futureDayText: Color,
    val todayBg: Color,
    val todayText: Color,
    val streakAccent: Color,
    val overflowBorder: Color,
    val overflowText: Color,
    val badgeBg: Color,
)

/**
 * Resolves a curated calendar theme from the habit's stored color.
 * Falls back to a dynamically-derived theme for unknown colors.
 */
fun calendarThemeFor(habitColor: Long): HabitCalendarTheme =
    CURATED_THEMES[habitColor] ?: buildFallbackTheme(Color(habitColor))

private val CURATED_THEMES: Map<Long, HabitCalendarTheme> = mapOf(

    // ── Charcoal ────────────────────────────────────────────────────────
    0xFF2E2D2BL to HabitCalendarTheme(
        name = "Charcoal",
        headerBg = Color(0xFF2E2D2B),
        headerGradientEnd = Color(0xFF3A3937),
        headerText = Color(0xFFF5F0EB),
        headerSubtle = Color(0xFFB8B3AD),
        completedDayBg = Color(0xFFF5F0EB),
        completedDayText = Color(0xFF2E2D2B),
        emptyDayBorder = Color(0xFF5C5A57),
        emptyDayText = Color(0xFF8A8580),
        skippedBorder = Color(0xFF5C5A57),
        skippedText = Color(0xFF6B6660),
        futureDayText = Color(0xFF4A4845),
        todayBg = Color(0xFFD2F34C),
        todayText = Color(0xFF1A1917),
        streakAccent = Color(0xFFD2F34C),
        overflowBorder = Color(0xFF444240),
        overflowText = Color(0xFF5C5A57),
        badgeBg = Color(0x30F5F0EB),
    ),

    // ── Teal ────────────────────────────────────────────────────────────
    0xFF6B8E8AL to HabitCalendarTheme(
        name = "Teal",
        headerBg = Color(0xFF4A7C77),
        headerGradientEnd = Color(0xFF5B8D88),
        headerText = Color(0xFFFFFFFF),
        headerSubtle = Color(0xFFCDE4E1),
        completedDayBg = Color(0xFFFFFFFF),
        completedDayText = Color(0xFF3A6A65),
        emptyDayBorder = Color(0xFF7DA9A5),
        emptyDayText = Color(0xFFADCCC9),
        skippedBorder = Color(0xFF7DA9A5),
        skippedText = Color(0xFF8ABAB6),
        futureDayText = Color(0xFF5E918C),
        todayBg = Color(0xFFE8FF8A),
        todayText = Color(0xFF2A5450),
        streakAccent = Color(0xFFE8FF8A),
        overflowBorder = Color(0xFF6B9E9A),
        overflowText = Color(0xFF7DA9A5),
        badgeBg = Color(0x30FFFFFF),
    ),

    // ── Bronze ──────────────────────────────────────────────────────────
    0xFF8B7355L to HabitCalendarTheme(
        name = "Bronze",
        headerBg = Color(0xFF7A6345),
        headerGradientEnd = Color(0xFF8E7555),
        headerText = Color(0xFFFFF8F0),
        headerSubtle = Color(0xFFD4C4AD),
        completedDayBg = Color(0xFFFFF8F0),
        completedDayText = Color(0xFF5A4830),
        emptyDayBorder = Color(0xFFA8937A),
        emptyDayText = Color(0xFFC4B49D),
        skippedBorder = Color(0xFFA8937A),
        skippedText = Color(0xFFB0A08A),
        futureDayText = Color(0xFF8A7A65),
        todayBg = Color(0xFFFFD666),
        todayText = Color(0xFF4A3A20),
        streakAccent = Color(0xFFFFD666),
        overflowBorder = Color(0xFF98835A),
        overflowText = Color(0xFFA8937A),
        badgeBg = Color(0x30FFF8F0),
    ),

    // ── Dusty Mauve ─────────────────────────────────────────────────────
    0xFF7B6B6BL to HabitCalendarTheme(
        name = "Mauve",
        headerBg = Color(0xFF6B5B5B),
        headerGradientEnd = Color(0xFF7D6D6D),
        headerText = Color(0xFFFFF5F5),
        headerSubtle = Color(0xFFD4C4C4),
        completedDayBg = Color(0xFFFFF5F5),
        completedDayText = Color(0xFF5A4A4A),
        emptyDayBorder = Color(0xFF9E8E8E),
        emptyDayText = Color(0xFFBDADAD),
        skippedBorder = Color(0xFF9E8E8E),
        skippedText = Color(0xFFA89898),
        futureDayText = Color(0xFF7A6A6A),
        todayBg = Color(0xFFFFB4C8),
        todayText = Color(0xFF4A3535),
        streakAccent = Color(0xFFFFB4C8),
        overflowBorder = Color(0xFF8E7E7E),
        overflowText = Color(0xFF9E8E8E),
        badgeBg = Color(0x30FFF5F5),
    ),

    // ── Burnt Orange ────────────────────────────────────────────────────
    0xFFE8673CL to HabitCalendarTheme(
        name = "Ember",
        headerBg = Color(0xFFD35A30),
        headerGradientEnd = Color(0xFFE06B40),
        headerText = Color(0xFFFFFFFF),
        headerSubtle = Color(0xFFFFD4C4),
        completedDayBg = Color(0xFFFFFFFF),
        completedDayText = Color(0xFFA84420),
        emptyDayBorder = Color(0xFFEE9070),
        emptyDayText = Color(0xFFFFBDA8),
        skippedBorder = Color(0xFFD48060),
        skippedText = Color(0xFFE09880),
        futureDayText = Color(0xFFCC7858),
        todayBg = Color(0xFFD2F34C),
        todayText = Color(0xFF5A2A10),
        streakAccent = Color(0xFFD2F34C),
        overflowBorder = Color(0xFFDD8060),
        overflowText = Color(0xFFEE9070),
        badgeBg = Color(0x30000000),
    ),

    // ── Dusty Rose ──────────────────────────────────────────────────────
    0xFF9B6B6BL to HabitCalendarTheme(
        name = "Rose",
        headerBg = Color(0xFF8E5555),
        headerGradientEnd = Color(0xFFA06565),
        headerText = Color(0xFFFFFFFF),
        headerSubtle = Color(0xFFE8CCCC),
        completedDayBg = Color(0xFFFFFFFF),
        completedDayText = Color(0xFF6E3E3E),
        emptyDayBorder = Color(0xFFBB8888),
        emptyDayText = Color(0xFFD4AAAA),
        skippedBorder = Color(0xFFBB8888),
        skippedText = Color(0xFFC49898),
        futureDayText = Color(0xFFA07070),
        todayBg = Color(0xFFFFE066),
        todayText = Color(0xFF5A2E2E),
        streakAccent = Color(0xFFFFE066),
        overflowBorder = Color(0xFFAA7878),
        overflowText = Color(0xFFBB8888),
        badgeBg = Color(0x30FFFFFF),
    ),

    // ── Slate ───────────────────────────────────────────────────────────
    0xFF5A6B7AL to HabitCalendarTheme(
        name = "Slate",
        headerBg = Color(0xFF455666),
        headerGradientEnd = Color(0xFF566878),
        headerText = Color(0xFFFFFFFF),
        headerSubtle = Color(0xFFB8C8D8),
        completedDayBg = Color(0xFFFFFFFF),
        completedDayText = Color(0xFF354656),
        emptyDayBorder = Color(0xFF7A8D9D),
        emptyDayText = Color(0xFFA0B0C0),
        skippedBorder = Color(0xFF7A8D9D),
        skippedText = Color(0xFF8898A8),
        futureDayText = Color(0xFF5A6A7A),
        todayBg = Color(0xFF80E8FF),
        todayText = Color(0xFF1A3040),
        streakAccent = Color(0xFF80E8FF),
        overflowBorder = Color(0xFF6A7D8D),
        overflowText = Color(0xFF7A8D9D),
        badgeBg = Color(0x30FFFFFF),
    ),

    // ── Olive ───────────────────────────────────────────────────────────
    0xFF8A7B5AL to HabitCalendarTheme(
        name = "Olive",
        headerBg = Color(0xFF6E6440),
        headerGradientEnd = Color(0xFF807550),
        headerText = Color(0xFFFFFFF0),
        headerSubtle = Color(0xFFD4CCA8),
        completedDayBg = Color(0xFFFFFFF0),
        completedDayText = Color(0xFF4E4428),
        emptyDayBorder = Color(0xFFA49870),
        emptyDayText = Color(0xFFC0B890),
        skippedBorder = Color(0xFFA49870),
        skippedText = Color(0xFFB0A880),
        futureDayText = Color(0xFF887E5A),
        todayBg = Color(0xFFE8FF66),
        todayText = Color(0xFF3A3418),
        streakAccent = Color(0xFFE8FF66),
        overflowBorder = Color(0xFF948A60),
        overflowText = Color(0xFFA49870),
        badgeBg = Color(0x30FFFFF0),
    ),

    // ── Muted Purple ────────────────────────────────────────────────────
    0xFF6B5B8AL to HabitCalendarTheme(
        name = "Amethyst",
        headerBg = Color(0xFF574878),
        headerGradientEnd = Color(0xFF685A8A),
        headerText = Color(0xFFFFFFFF),
        headerSubtle = Color(0xFFD0C4E8),
        completedDayBg = Color(0xFFFFFFFF),
        completedDayText = Color(0xFF443668),
        emptyDayBorder = Color(0xFF8A7AAA),
        emptyDayText = Color(0xFFAEA0CC),
        skippedBorder = Color(0xFF8A7AAA),
        skippedText = Color(0xFF9888B8),
        futureDayText = Color(0xFF6A5A88),
        todayBg = Color(0xFFD4AAFF),
        todayText = Color(0xFF2A1E44),
        streakAccent = Color(0xFFD4AAFF),
        overflowBorder = Color(0xFF7A6A9A),
        overflowText = Color(0xFF8A7AAA),
        badgeBg = Color(0x30FFFFFF),
    ),

    // ── Indigo ──────────────────────────────────────────────────────────
    0xFF3F51B5L to HabitCalendarTheme(
        name = "Indigo",
        headerBg = Color(0xFF3040A0),
        headerGradientEnd = Color(0xFF4050B0),
        headerText = Color(0xFFFFFFFF),
        headerSubtle = Color(0xFFC0C8F0),
        completedDayBg = Color(0xFFFFFFFF),
        completedDayText = Color(0xFF283080),
        emptyDayBorder = Color(0xFF6878CC),
        emptyDayText = Color(0xFF98A0DD),
        skippedBorder = Color(0xFF6878CC),
        skippedText = Color(0xFF7888D0),
        futureDayText = Color(0xFF5060B8),
        todayBg = Color(0xFFFFE066),
        todayText = Color(0xFF1A2060),
        streakAccent = Color(0xFFFFE066),
        overflowBorder = Color(0xFF5868BB),
        overflowText = Color(0xFF6878CC),
        badgeBg = Color(0x30FFFFFF),
    ),

    // ── Blue ────────────────────────────────────────────────────────────
    0xFF2196F3L to HabitCalendarTheme(
        name = "Ocean",
        headerBg = Color(0xFF1880DD),
        headerGradientEnd = Color(0xFF2090EE),
        headerText = Color(0xFFFFFFFF),
        headerSubtle = Color(0xFFB0D8FF),
        completedDayBg = Color(0xFFFFFFFF),
        completedDayText = Color(0xFF0C5AA0),
        emptyDayBorder = Color(0xFF60AAEE),
        emptyDayText = Color(0xFF90C8FF),
        skippedBorder = Color(0xFF60AAEE),
        skippedText = Color(0xFF70B8FF),
        futureDayText = Color(0xFF4098E8),
        todayBg = Color(0xFFD2F34C),
        todayText = Color(0xFF0A3A70),
        streakAccent = Color(0xFFD2F34C),
        overflowBorder = Color(0xFF50A0DD),
        overflowText = Color(0xFF60AAEE),
        badgeBg = Color(0x30FFFFFF),
    ),

    // ── Brown ───────────────────────────────────────────────────────────
    0xFF795548L to HabitCalendarTheme(
        name = "Umber",
        headerBg = Color(0xFF664438),
        headerGradientEnd = Color(0xFF785548),
        headerText = Color(0xFFFFF5F0),
        headerSubtle = Color(0xFFD4BAB0),
        completedDayBg = Color(0xFFFFF5F0),
        completedDayText = Color(0xFF4A3028),
        emptyDayBorder = Color(0xFF9E8070),
        emptyDayText = Color(0xFFC0A898),
        skippedBorder = Color(0xFF9E8070),
        skippedText = Color(0xFFAA9080),
        futureDayText = Color(0xFF806858),
        todayBg = Color(0xFFFFCC44),
        todayText = Color(0xFF3A2418),
        streakAccent = Color(0xFFFFCC44),
        overflowBorder = Color(0xFF8E7060),
        overflowText = Color(0xFF9E8070),
        badgeBg = Color(0x30FFF5F0),
    ),
)

/**
 * Fallback theme derived from the color when it doesn't match any preset.
 */
private fun buildFallbackTheme(color: Color): HabitCalendarTheme {
    val isLight = color.luminance() > 0.4f
    val contentColor = if (isLight) Color(0xFF1A1917) else Color.White
    return HabitCalendarTheme(
        name = "Custom",
        headerBg = color,
        headerGradientEnd = color.copy(alpha = 0.92f),
        headerText = contentColor,
        headerSubtle = contentColor.copy(alpha = 0.6f),
        completedDayBg = contentColor.copy(alpha = 0.85f),
        completedDayText = color,
        emptyDayBorder = contentColor.copy(alpha = 0.25f),
        emptyDayText = contentColor.copy(alpha = 0.5f),
        skippedBorder = contentColor.copy(alpha = 0.3f),
        skippedText = contentColor.copy(alpha = 0.35f),
        futureDayText = contentColor.copy(alpha = 0.2f),
        todayBg = Color(0xFFD2F34C),
        todayText = Color(0xFF1A1917),
        streakAccent = Color(0xFFD2F34C),
        overflowBorder = contentColor.copy(alpha = 0.15f),
        overflowText = contentColor.copy(alpha = 0.25f),
        badgeBg = Color.Black.copy(alpha = 0.18f),
    )
}

private fun Color.luminance(): Float {
    val r = red; val g = green; val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
