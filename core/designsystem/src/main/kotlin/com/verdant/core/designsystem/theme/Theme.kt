package com.verdant.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private val LightColorScheme = lightColorScheme(
    primary = VerdantGreen40,
    onPrimary = VerdantNeutral99,
    primaryContainer = VerdantGreen90,
    secondary = VerdantTeal40,
    onSecondary = VerdantNeutral99,
    secondaryContainer = VerdantTeal90,
    error = VerdantError40,
    onError = VerdantNeutral99,
    errorContainer = VerdantError90,
    background = VerdantNeutral99,
    surface = VerdantNeutral99,
    onBackground = VerdantNeutral10,
    onSurface = VerdantNeutral10,
)

private val DarkColorScheme = darkColorScheme(
    primary = VerdantGreen80,
    onPrimary = VerdantGreen20,
    primaryContainer = VerdantGreen30,
    secondary = VerdantTeal80,
    onSecondary = VerdantTeal10,
    secondaryContainer = VerdantTeal40,
    error = VerdantError80,
    onError = VerdantError10,
    errorContainer = VerdantError40,
    background = VerdantNeutral10,
    surface = VerdantNeutral10,
    onBackground = VerdantNeutral90,
    onSurface = VerdantNeutral90,
)

@Composable
fun VerdantTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
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
        content = content,
    )
}
