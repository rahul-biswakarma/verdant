package com.verdant.feature.finance.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verdant.core.model.CategorySpend
import com.verdant.feature.finance.chart.CategoryBreakdownChart
import com.verdant.feature.finance.chart.SpendingLineChart
import compose.icons.TablerIcons
import compose.icons.tablericons.ChartLine

@Composable
fun TrendsTab(
    monthlyTotals: Map<String, Double>,
    categoryBreakdown: List<CategorySpend>,
    monthOverMonthChange: Double?,
    modifier: Modifier = Modifier,
) {
    // Empty state — need at least 2 months for trends
    if (monthlyTotals.size < 2) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = TablerIcons.ChartLine,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = "Trends will appear once you have at least 2 months of data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Month-over-month summary card
        if (monthOverMonthChange != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Month over month",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    val changeText = if (monthOverMonthChange >= 0) {
                        "+${String.format("%.1f", monthOverMonthChange)}%"
                    } else {
                        "${String.format("%.1f", monthOverMonthChange)}%"
                    }
                    Text(
                        text = "$changeText vs last month",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (monthOverMonthChange <= 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // Monthly spending line chart
        Text(
            text = "Monthly Spending",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SpendingLineChart(
            monthlyTotals = monthlyTotals,
            modifier = Modifier.fillMaxWidth(),
        )

        // Category breakdown
        if (categoryBreakdown.isNotEmpty()) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            CategoryBreakdownChart(
                categories = categoryBreakdown,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
