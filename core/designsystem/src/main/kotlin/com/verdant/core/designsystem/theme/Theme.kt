package com.verdant.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private val LightColorScheme = lightColorScheme(
    primary = MutedSage,
    onPrimary = WarmWhite,
    primaryContainer = LightSage,
    onPrimaryContainer = DeepCharcoal,
    secondary = DustyMauve,
    onSecondary = WarmWhite,
    secondaryContainer = LightMauve,
    onSecondaryContainer = DeepCharcoal,
    tertiary = BurntOrange,
    onTertiary = WarmWhite,
    tertiaryContainer = LightPeach,
    onTertiaryContainer = DeepCharcoal,
    error = WarmRed,
    onError = WarmWhite,
    errorContainer = LightWarmRed,
    onErrorContainer = DeepCharcoal,
    background = WarmCream,
    onBackground = DeepCharcoal,
    surface = WarmWhite,
    onSurface = DeepCharcoal,
    surfaceVariant = WarmGray,
    onSurfaceVariant = DustyMauve,
    outline = WarmGray,
    outlineVariant = WarmGray.copy(alpha = 0.5f),
)

private val DarkColorScheme = darkColorScheme(
    primary = LightSageGreen,
    onPrimary = DeepWarmBlack,
    primaryContainer = DarkSage,
    onPrimaryContainer = LightSage,
    secondary = LightMauve,
    onSecondary = DeepWarmBlack,
    secondaryContainer = DarkMauve,
    onSecondaryContainer = WarmOffWhite,
    tertiary = LightOrange,
    onTertiary = DeepWarmBlack,
    tertiaryContainer = DarkPeach,
    onTertiaryContainer = LightPeach,
    error = LightWarmRed,
    onError = DarkOnError,
    errorContainer = WarmRed,
    onErrorContainer = LightWarmRed,
    background = DeepWarmBlack,
    onBackground = WarmOffWhite,
    surface = DarkWarmBrown,
    onSurface = WarmOffWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = WarmGray,
    outline = DarkSurfaceVariant,
    outlineVariant = DarkSurfaceVariant.copy(alpha = 0.5f),
)

val VerdantShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun VerdantTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VerdantTypography,
        shapes = VerdantShapes,
        content = content,
    )
}
