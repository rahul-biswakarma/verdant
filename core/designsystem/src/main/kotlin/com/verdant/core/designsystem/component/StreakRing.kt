package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdant.core.designsystem.theme.WarmCharcoal
import com.verdant.core.designsystem.theme.VerdantTheme

/**
 * Circular progress ring with a streak count displayed in the center.
 *
 * @param progress Value in [0, 1].
 * @param streakCount Number shown in the center of the ring.
 * @param color Arc fill color.
 * @param size Outer diameter of the ring.
 */
@Composable
fun StreakRing(
    progress: Float,
    streakCount: Int,
    color: Color,
    size: Dp = 48.dp,
    strokeWidth: Dp = 6.dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        CompletionRing(
            progress = progress,
            color = color,
            size = size,
            strokeWidth = strokeWidth,
        )
        Text(
            text = streakCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.3f).sp,
            color = color,
        )
    }
}

@Preview(name = "StreakRing – light", showBackground = true)
@Composable
private fun StreakRingLightPreview() {
    VerdantTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            StreakRing(progress = 0.3f, streakCount = 3, color = WarmCharcoal)
            StreakRing(progress = 0.65f, streakCount = 12, color = WarmCharcoal)
            StreakRing(progress = 1f, streakCount = 30, color = WarmCharcoal)
        }
    }
}

@Preview(
    name = "StreakRing – dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun StreakRingDarkPreview() {
    VerdantTheme(dynamicColor = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            StreakRing(progress = 0.5f, streakCount = 7, color = WarmCharcoal)
            StreakRing(progress = 0.8f, streakCount = 21, color = WarmCharcoal)
        }
    }
}
