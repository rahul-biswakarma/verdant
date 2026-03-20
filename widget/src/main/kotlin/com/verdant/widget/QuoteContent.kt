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
 * Quote widget composable (2×2).
 *
 * Displays a full-size bitmap of a gradient quote card generated daily by
 * [WidgetBitmapUtils.generateQuoteCard] in [WidgetUpdateWorker].
 */
@androidx.compose.runtime.Composable
internal fun QuoteContent() {
    val prefs = currentState<Preferences>()
    val path  = prefs[WidgetPreferencesKeys.BITMAP_PATH]
    val bmp   = path?.let { WidgetBitmapUtils.loadBitmap(it) }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20))   // fallback bg until bitmap loads
            .cornerRadius(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(
                provider           = ImageProvider(bmp),
                contentDescription = "Motivational quote",
                contentScale       = ContentScale.Crop,
                modifier           = GlanceModifier.fillMaxSize(),
            )
        } else {
            Text(
                text  = "🌱 Verdant",
                style = TextStyle(
                    color    = ColorProvider(Color.White.copy(alpha = 0.6f)),
                    fontSize = 11.sp,
                ),
            )
        }
    }
}
