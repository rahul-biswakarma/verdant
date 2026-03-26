package com.verdant.core.designsystem.theme

import androidx.compose.ui.graphics.Color
val WarmCream         = Color(0xFFF5F0EB)   // background
val WarmWhite         = Color(0xFFFAF7F3)   // surface
val WarmGray          = Color(0xFFE8E2DB)   // surface variant
val MutedSage         = Color(0xFF5A7A60)   // primary
val LightSage         = Color(0xFFD4E8D4)   // primary container
val DustyMauve        = Color(0xFF7B6B6B)   // secondary
val LightMauve        = Color(0xFFE0D5D5)   // secondary container
val BurntOrange       = Color(0xFFE8673C)   // tertiary / accent
val LightPeach        = Color(0xFFFDDDD0)   // tertiary container
val DeepCharcoal      = Color(0xFF1A1917)   // on-background / on-surface
val WarmRed           = Color(0xFFC4453A)   // error
val LightWarmRed      = Color(0xFFFCDAD7)   // error container
val DeepWarmBlack     = Color(0xFF141311)   // background
val DarkWarmBrown     = Color(0xFF1E1C1A)   // surface
val DarkSurfaceVariant = Color(0xFF2A2826)  // surface variant
val LightSageGreen    = Color(0xFF8FB996)   // primary
val DarkSage          = Color(0xFF3D5442)   // primary container
val DarkMauve         = Color(0xFF4A3E3E)   // secondary container
val LightOrange       = Color(0xFFF09070)   // tertiary
val DarkPeach         = Color(0xFF5C3328)   // tertiary container
val WarmOffWhite      = Color(0xFFE8E2DB)   // on-background / on-surface
val DarkOnError       = Color(0xFF3B1211)   // on-error
val GridEmptyLight  = Color(0xFFE8E2DB)
val GridLevel1Light = Color(0xFFC4D8C4)
val GridLevel2Light = Color(0xFF8FB996)
val GridLevel3Light = Color(0xFF5A7A60)
val GridLevel4Light = Color(0xFF3D5442)

val GridEmptyDark   = Color(0xFF1E1C1A)
val GridLevel1Dark  = Color(0xFF2A3D2E)
val GridLevel2Dark  = Color(0xFF3D5442)
val GridLevel3Dark  = Color(0xFF5A7A60)
val GridLevel4Dark  = Color(0xFF8FB996)

/**
 * Returns one of the 5 grid-level colors based on [intensity] and theme.
 * Used as the default color for habits whose primary color is [MutedSage].
 */
fun gridCellColor(intensity: Float, isDark: Boolean): Color = when {
    intensity <= 0f    -> if (isDark) GridEmptyDark   else GridEmptyLight
    intensity <= 0.25f -> if (isDark) GridLevel1Dark  else GridLevel1Light
    intensity <= 0.50f -> if (isDark) GridLevel2Dark  else GridLevel2Light
    intensity <= 0.75f -> if (isDark) GridLevel3Dark  else GridLevel3Light
    else               -> if (isDark) GridLevel4Dark  else GridLevel4Light
}
