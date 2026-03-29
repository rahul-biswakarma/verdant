package com.verdant.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ── Light palette ──────────────────────────────────────────────
val WarmCream         = Color(0xFFF5F0EB)   // background
val WarmWhite         = Color(0xFFFAF7F3)   // surface
val WarmGray          = Color(0xFFE8E2DB)   // surface variant
val WarmCharcoal      = Color(0xFF2E2D2B)   // primary
val LightCharcoal     = Color(0xFFD6D2CC)   // primary container
val MediumCharcoal    = Color(0xFF5C5A57)   // secondary
val PaleWarm          = Color(0xFFE8E2DB)   // secondary container
val BurntOrange       = Color(0xFFE8673C)   // tertiary / accent
val LightPeach        = Color(0xFFFDDDD0)   // tertiary container
val DeepCharcoal      = Color(0xFF1A1917)   // on-background / on-surface
val WarmRed           = Color(0xFFC4453A)   // error
val LightWarmRed      = Color(0xFFFCDAD7)   // error container

// ── Dark palette ───────────────────────────────────────────────
val DeepWarmBlack     = Color(0xFF141311)   // background
val DarkWarmBrown     = Color(0xFF1E1C1A)   // surface
val DarkSurfaceVariant = Color(0xFF2A2826)  // surface variant
val WarmStone         = Color(0xFFB8B3AD)   // primary
val DarkCharcoal      = Color(0xFF3A3836)   // primary container
val LightStone        = Color(0xFF9E9A95)   // secondary
val DarkMauve         = Color(0xFF4A3E3E)   // secondary container
val LightOrange       = Color(0xFFF09070)   // tertiary
val DarkPeach         = Color(0xFF5C3328)   // tertiary container
val WarmOffWhite      = Color(0xFFE8E2DB)   // on-background / on-surface
val DarkOnError       = Color(0xFF3B1211)   // on-error

// ── Grid intensity scale (charcoal-based) ──────────────────────
val GridEmptyLight  = Color(0xFFE8E2DB)
val GridLevel1Light = Color(0xFFD0CBC5)
val GridLevel2Light = Color(0xFF9E9A95)
val GridLevel3Light = Color(0xFF5C5A57)
val GridLevel4Light = Color(0xFF2E2D2B)

val GridEmptyDark   = Color(0xFF1E1C1A)
val GridLevel1Dark  = Color(0xFF2A2826)
val GridLevel2Dark  = Color(0xFF3A3836)
val GridLevel3Dark  = Color(0xFF5C5A57)
val GridLevel4Dark  = Color(0xFFB8B3AD)

// ── Default habit color ────────────────────────────────────────
val DefaultHabitColor = 0xFF2E2D2BL

/**
 * Returns one of the 5 grid-level colors based on [intensity] and theme.
 * Used as the default color for habits whose primary color is [WarmCharcoal].
 */
fun gridCellColor(intensity: Float, isDark: Boolean): Color = when {
    intensity <= 0f    -> if (isDark) GridEmptyDark   else GridEmptyLight
    intensity <= 0.25f -> if (isDark) GridLevel1Dark  else GridLevel1Light
    intensity <= 0.50f -> if (isDark) GridLevel2Dark  else GridLevel2Light
    intensity <= 0.75f -> if (isDark) GridLevel3Dark  else GridLevel3Light
    else               -> if (isDark) GridLevel4Dark  else GridLevel4Light
}
