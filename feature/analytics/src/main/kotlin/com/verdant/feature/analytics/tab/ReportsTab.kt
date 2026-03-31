package com.verdant.feature.analytics.tab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import compose.icons.tablericons.Calendar
import compose.icons.tablericons.CalendarEvent
import compose.icons.tablericons.ChevronDown
import compose.icons.tablericons.ChevronUp
import compose.icons.tablericons.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.verdant.feature.analytics.ReportEntry
import com.verdant.feature.analytics.ReportsState
import java.text.DateFormat
import java.util.Date

@Composable
fun ReportsTab(
    state: ReportsState,
    onGenerateWeekly: () -> Unit,
    onGenerateMonthly: () -> Unit,
    onToggleExpand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(title = "Generate Reports") {
            Text(
                text  = "AI-powered summaries of your habit performance, " +
                        "with patterns and personalised suggestions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val isGeneratingWeekly  = state is ReportsState.GeneratingWeekly
                val isGeneratingMonthly = state is ReportsState.GeneratingMonthly
                val isBusy = isGeneratingWeekly || isGeneratingMonthly

                Button(
                    onClick  = onGenerateWeekly,
                    enabled  = !isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isGeneratingWeekly) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(TablerIcons.Calendar, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.size(6.dp))
                    Text("Weekly", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick  = onGenerateMonthly,
                    enabled  = !isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isGeneratingMonthly) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(TablerIcons.CalendarEvent, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.size(6.dp))
                    Text("Monthly", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (state is ReportsState.Error) {
            SectionCard(title = "Error") {
                Text(
                    text  = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        when (state) {
            is ReportsState.Idle -> {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(Modifier.height(16.dp))
                    Icon(
                        imageVector = TablerIcons.Stars,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    )
                    Text(
                        text      = "Generate your first report above to see AI-powered insights",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is ReportsState.Ready -> {
                if (state.reports.isNotEmpty()) {
                    Text(
                        text  = "Past Reports",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.reports.forEach { report ->
                        ReportCard(
                            report     = report,
                            expanded   = state.expandedReportId == report.id,
                            onToggle   = { onToggleExpand(report.id) },
                        )
                    }
                }
            }

            else -> { /* GeneratingWeekly / GeneratingMonthly / Error — buttons above handle it */ }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReportCard(
    report: ReportEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(report.generatedAt))

    SectionCard(
        title    = report.title,
        modifier = modifier.clickable(onClick = onToggle),
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (expanded) TablerIcons.ChevronUp else TablerIcons.ChevronDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text  = report.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
