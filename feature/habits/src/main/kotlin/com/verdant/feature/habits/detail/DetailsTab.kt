package com.verdant.feature.habits.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DetailsTab(
    currentStreak: Int,
    longestStreak: Int,
    completionRate: Float,
    totalEntries: Int,
    averageValue: Double?,
    unit: String?,
    modifier: Modifier = Modifier,
) {
    val items = buildList {
        add(StatItem("🔥", "$currentStreak", "Current streak", "days"))
        add(StatItem("🏆", "$longestStreak", "Best streak", "days"))
        add(StatItem("✅", "${(completionRate * 100).toInt()}%", "30-day rate", ""))
        add(StatItem("📊", "$totalEntries", "Total done", ""))
        if (averageValue != null) {
            add(StatItem("📈", "%.1f".format(averageValue), "Average", unit ?: ""))
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items.size) { index ->
            val item = items[index]
            DetailStatCard(
                emoji = item.emoji,
                value = item.value,
                label = item.label,
                suffix = item.suffix,
            )
        }
    }
}

private data class StatItem(val emoji: String, val value: String, val label: String, val suffix: String)

@Composable
private fun DetailStatCard(emoji: String, value: String, label: String, suffix: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (suffix.isNotBlank()) {
                Text(
                    suffix,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
