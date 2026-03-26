package com.verdant.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Summary ring widget composable (2×2).
 *
 * Loads a pre-generated PNG bitmap from [WidgetPreferencesKeys.BITMAP_PATH]
 * (written by [WidgetUpdateWorker]) and displays it full-size.
 */
@androidx.compose.runtime.Composable
internal fun SummaryContent() {
    val prefs = currentState<Preferences>()
    val path  = prefs[WidgetPreferencesKeys.BITMAP_PATH]
    val bmp   = path?.let { WidgetBitmapUtils.loadBitmap(it) }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .cornerRadius(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(
                provider     = ImageProvider(bmp),
                contentDescription = "Habit completion ring",
                contentScale = ContentScale.Fit,
                modifier     = GlanceModifier.fillMaxSize(),
            )
        } else {
            Text(
                text  = "Loading…",
                style = TextStyle(
                    color    = ColorProvider(Color(0xFF9E9E9E)),
                    fontSize = 10.sp,
                ),
            )
        }
    }
}
