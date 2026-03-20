package com.verdant.feature.analytics.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import compose.icons.TablerIcons
import compose.icons.tablericons.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.verdant.core.ai.Correlation
import com.verdant.core.model.Habit
import com.verdant.feature.analytics.CorrelationsState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CorrelationsTab(
    habits: List<Habit>,
    state: CorrelationsState,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (habits.size < 2) {
        EmptyAnalyticsState(
            message = "Add at least 2 habits to discover correlations between them",
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Header card ───────────────────────────────────────────────────────
        SectionCard(title = "Habit Correlations") {
            Text(
                text  = "Discover which of your habits tend to be done together, " +
                        "or which ones conflict with each other. " +
                        "Analysis uses your last 30 days of data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Content by state ─────────────────────────────────────────────────
        when (state) {
            is CorrelationsState.Idle -> {
                Column(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(Modifier.height(16.dp))
                    Icon(
                        imageVector = TablerIcons.Stars,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    )
                    Text(
                        text      = "Find hidden connections between your habits",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onGenerate) {
                        Icon(TablerIcons.Stars, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Analyse Correlations")
                    }
                }
            }

            is CorrelationsState.Loading -> {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Text(
                        text  = "Analysing your habit patterns…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is CorrelationsState.Success -> {
                if (state.correlations.isEmpty()) {
                    SectionCard(title = "Results") {
                        Text(
                            text  = "No significant correlations found in your 30-day window. " +
                                    "Try again after logging more habits consistently.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    state.correlations.forEach { correlation ->
                        CorrelationCard(correlation)
                    }
                    // Re-generate button
                    Button(
                        onClick  = onGenerate,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("Refresh")
                    }
                }
            }

            is CorrelationsState.Error -> {
                SectionCard(title = "Error") {
                    Text(
                        text  = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick  = onGenerate,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Try Again")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CorrelationCard(correlation: Correlation) {
    val strength = correlation.strength
    val (label, color) = when {
        strength > 0.6f  -> "Strong positive" to MaterialTheme.colorScheme.primary
        strength > 0.3f  -> "Moderate positive" to MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        strength < -0.6f -> "Strong negative" to MaterialTheme.colorScheme.error
        strength < -0.3f -> "Moderate negative" to MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        else             -> "Weak" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    SectionCard(title = "${correlation.habit1Name}  ↔  ${correlation.habit2Name}") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Strength bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LinearProgressIndicator(
                    progress = { abs(strength).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f),
                    color    = color,
                )
                Text(
                    text       = "${(abs(strength) * 100).roundToInt()}%",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = color,
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
                Text(
                    text  = if (strength >= 0) "↑ Positive" else "↓ Negative",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }

            if (correlation.description.isNotBlank()) {
                Text(
                    text  = correlation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
