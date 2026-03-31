package com.verdant.feature.finance.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verdant.core.model.CategorySpend
import com.verdant.core.model.MonthlyPrediction
import java.text.NumberFormat
import java.util.Locale

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

@Composable
fun OverviewTab(
    totalSpent: Double,
    totalIncome: Double,
    categoryBreakdown: List<CategorySpend>,
    prediction: MonthlyPrediction?,
    monthOverMonthChange: Double?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Spending / Income summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryCard(
                title = "Spent",
                amount = totalSpent,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                title = "Income",
                amount = totalIncome,
                modifier = Modifier.weight(1f),
            )
        }

        // Month-over-month change
        if (monthOverMonthChange != null) {
            val changeText = if (monthOverMonthChange >= 0) {
                "+${String.format("%.1f", monthOverMonthChange)}% vs last month"
            } else {
                "${String.format("%.1f", monthOverMonthChange)}% vs last month"
            }
            Text(
                text = changeText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (monthOverMonthChange <= 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )
        }

        // Category breakdown
        if (categoryBreakdown.isNotEmpty()) {
            Text(
                text = "By category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            categoryBreakdown.forEach { cat ->
                CategoryRow(
                    category = cat.category,
                    amount = cat.amount,
                    percentage = cat.percentage,
                )
            }
        }

        // Prediction card
        if (prediction != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Next month prediction",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = currencyFormat.format(prediction.predictedTotal),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (prediction.unusualItems.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = prediction.unusualItems.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Empty state
        if (totalSpent == 0.0 && totalIncome == 0.0) {
            Text(
                text = "No transactions yet this month. Grant SMS access or add transactions manually.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                text = currencyFormat.format(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: String,
    amount: Double,
    percentage: Float,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(category, style = MaterialTheme.typography.bodyMedium)
            Text(currencyFormat.format(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}
