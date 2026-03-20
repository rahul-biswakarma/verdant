package com.verdant.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Verdant green-based palette
val VerdantGreen10 = Color(0xFF002201)
val VerdantGreen20 = Color(0xFF003A06)
val VerdantGreen30 = Color(0xFF00540D)
val VerdantGreen40 = Color(0xFF30A14E) // Brand primary
val VerdantGreen80 = Color(0xFF7DDC6E)
val VerdantGreen90 = Color(0xFF99F883)

val VerdantTeal10 = Color(0xFF001F23)
val VerdantTeal40 = Color(0xFF00696F)
val VerdantTeal80 = Color(0xFF4DD8E1)
val VerdantTeal90 = Color(0xFF97F0F9)

val VerdantNeutral10 = Color(0xFF1A1C19)
val VerdantNeutral90 = Color(0xFFE2E3DD)
val VerdantNeutral99 = Color(0xFFF8FAF2)

val VerdantError10 = Color(0xFF410002)
val VerdantError40 = Color(0xFFBA1A1A)
val VerdantError80 = Color(0xFFFFB4AB)
val VerdantError90 = Color(0xFFFFDAD6)

// GitHub-style contribution grid — dark theme
val GridEmptyDark   = Color(0xFF161B22)
val GridLevel1Dark  = Color(0xFF0E4429)
val GridLevel2Dark  = Color(0xFF006D32)
val GridLevel3Dark  = Color(0xFF26A641)
val GridLevel4Dark  = Color(0xFF39D353)

// GitHub-style contribution grid — light theme
val GridEmptyLight  = Color(0xFFEBEDF0)
val GridLevel1Light = Color(0xFF9BE9A8)
val GridLevel2Light = Color(0xFF40C463)
val GridLevel3Light = Color(0xFF30A14E)
val GridLevel4Light = Color(0xFF216E39)

/**
 * Returns one of the 5 grid-level colors based on [intensity] and theme.
 * Used as the default color for habits whose primary color is VerdantGreen40.
 */
fun gridCellColor(intensity: Float, isDark: Boolean): Color = when {
    intensity <= 0f    -> if (isDark) GridEmptyDark   else GridEmptyLight
    intensity <= 0.25f -> if (isDark) GridLevel1Dark  else GridLevel1Light
    intensity <= 0.50f -> if (isDark) GridLevel2Dark  else GridLevel2Light
    intensity <= 0.75f -> if (isDark) GridLevel3Dark  else GridLevel3Light
    else               -> if (isDark) GridLevel4Dark  else GridLevel4Light
}
