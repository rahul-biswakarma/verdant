package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import compose.icons.TablerIcons
import compose.icons.tablericons.Flame
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.BurntOrange
import com.verdant.core.designsystem.theme.VerdantTheme

/**
 * Compact streak indicator: flame icon + count.
 * Hidden when [count] is zero.
 */
@Composable
fun StreakBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = TablerIcons.Flame,
            contentDescription = null,
            tint = BurntOrange,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = BurntOrange,
        )
    }
}
@Preview(name = "StreakBadge – light", showBackground = true)
@Composable
private fun StreakBadgeLightPreview() {
    VerdantTheme { StreakBadge(count = 21) }
}

@Preview(
    name = "StreakBadge – dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun StreakBadgeDarkPreview() {
    VerdantTheme { StreakBadge(count = 7) }
}
