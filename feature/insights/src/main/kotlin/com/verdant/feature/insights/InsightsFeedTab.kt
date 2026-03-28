package com.verdant.feature.insights

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.Bulb
import compose.icons.tablericons.ChartBar
import compose.icons.tablericons.ChartDots
import compose.icons.tablericons.Flame
import compose.icons.tablericons.Stars
import compose.icons.tablericons.TrendingUp
import compose.icons.tablericons.Trophy
import compose.icons.tablericons.X
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.MutedSage
import com.verdant.core.model.AIInsight
import com.verdant.core.model.InsightType
import java.text.DateFormat
import java.util.Date

@Composable
fun InsightsFeedTab(
    state: FeedState,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.isEmpty -> {
            Box(
                modifier = modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector        = TablerIcons.Stars,
                        contentDescription = null,
                        modifier           = Modifier.size(56.dp),
                        tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    )
                    Text(
                        text      = "Your AI insights will appear here",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text      = "Log habits consistently and check back tomorrow for your first personalised insight.",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        else -> {
            LazyColumn(
                modifier            = modifier.fillMaxSize(),
                contentPadding      = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.insights,
                    key   = { it.id },
                ) { insight ->
                    InsightCard(
                        insight   = insight,
                        onDismiss = { onDismiss(insight.id) },
                        modifier  = Modifier.animateItem(),
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: AIInsight,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, iconColor, label) = remember(insight.type) {
        insightMeta(insight.type)
    }
    val dateStr = remember(insight.generatedAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(insight.generatedAt))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pill icon
                Box(
                    modifier          = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment  = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = iconColor,
                        modifier           = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = label,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = iconColor,
                    )
                    Text(
                        text  = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector        = TablerIcons.X,
                        contentDescription = "Dismiss",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text  = insight.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
private data class InsightMeta(
    val icon: ImageVector,
    val color: Color,
    val label: String,
)

private fun insightMeta(type: InsightType): InsightMeta = when (type) {
    InsightType.DAILY_MOTIVATION  -> InsightMeta(TablerIcons.Stars,          MutedSage,        "Daily Motivation")
    InsightType.STREAK_ALERT      -> InsightMeta(TablerIcons.Flame,   Color(0xFFFF6D00),     "Streak Alert")
    InsightType.PATTERN_RECOGNITION -> InsightMeta(TablerIcons.ChartDots,            Color(0xFF7C4DFF),     "Pattern Detected")
    InsightType.CORRELATION       -> InsightMeta(TablerIcons.TrendingUp,            Color(0xFF0288D1),     "Correlation")
    InsightType.WEEKLY_SUMMARY    -> InsightMeta(TablerIcons.ChartBar,              Color(0xFF00897B),     "Weekly Summary")
    InsightType.MONTHLY_SUMMARY   -> InsightMeta(TablerIcons.ChartBar,              Color(0xFF1565C0),     "Monthly Summary")
    InsightType.SUGGESTION        -> InsightMeta(TablerIcons.Bulb,             Color(0xFFF9A825),     "Suggestion")
    else                          -> InsightMeta(TablerIcons.Stars,            Color(0xFF90A4AE),     type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() })
}
