package com.verdant.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.VerdantTheme
import com.verdant.core.model.Label

/**
 * Colored chip representing a habit [Label].
 */
@Composable
fun LabelChip(
    label: Label,
    modifier: Modifier = Modifier,
) {
    val chipColor = Color(label.color.toULong())

    Surface(
        color = chipColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(chipColor),
            )
            Text(
                text = label.name,
                style = MaterialTheme.typography.labelSmall,
                color = chipColor,
            )
        }
    }
}
private val previewLabels = listOf(
    Label(id = "1", name = "Health", color = 0xFF4CAF50),
    Label(id = "2", name = "Work",   color = 0xFF2196F3),
    Label(id = "3", name = "Focus",  color = 0xFFFF9800),
)

@Preview(name = "LabelChip – light", showBackground = true)
@Composable
private fun LabelChipLightPreview() {
    VerdantTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            previewLabels.forEach { LabelChip(label = it) }
        }
    }
}

@Preview(
    name = "LabelChip – dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LabelChipDarkPreview() {
    VerdantTheme(dynamicColor = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            previewLabels.forEach { LabelChip(label = it) }
        }
    }
}
