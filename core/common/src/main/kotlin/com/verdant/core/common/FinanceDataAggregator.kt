package com.verdant.core.common

import com.verdant.core.model.CategorySpend
import com.verdant.core.model.MerchantSpend
import com.verdant.core.model.MonthlyPrediction
import com.verdant.core.model.MonthlySpendingSummary
import com.verdant.core.model.RecurringTransaction
import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import com.verdant.core.prediction.SpendingForecaster
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceDataAggregator @Inject constructor(
    private val forecaster: SpendingForecaster,
) {

    fun aggregateForMonth(
        transactions: List<Transaction>,
        month: YearMonth,
    ): MonthlySpendingSummary {
        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        val credits = transactions.filter { it.type == TransactionType.CREDIT }

        val totalSpent = debits.sumOf { it.amount }
        val totalIncome = credits.sumOf { it.amount }

        val categoryMap = debits
            .groupBy { it.category ?: "OTHER" }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        val categoryBreakdown = categoryMap.entries
            .sortedByDescending { it.value }
            .map { (cat, amount) ->
                CategorySpend(
                    category = cat,
                    amount = amount,
                    percentage = if (totalSpent > 0) (amount / totalSpent * 100).toFloat() else 0f,
                )
            }

        val topMerchants = debits
            .filter { it.merchant != null }
            .groupBy { it.merchant!! }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } to txns.size }
            .entries
            .sortedByDescending { it.value.first }
            .take(5)
            .map { (merchant, pair) ->
                MerchantSpend(
                    merchant = merchant,
                    amount = pair.first,
                    transactionCount = pair.second,
                )
            }

        return MonthlySpendingSummary(
            month = month,
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            categoryBreakdown = categoryBreakdown,
            topMerchants = topMerchants,
            comparedToLastMonth = null,
        )
    }

    fun monthlyTotals(
        transactions: List<Transaction>,
        months: Int = 6,
    ): Map<String, Double> {
        val zone = ZoneId.systemDefault()
        return transactions
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { txn ->
                val date = Instant.ofEpochMilli(txn.transactionDate).atZone(zone).toLocalDate()
                YearMonth.from(date).toString()
            }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
    }

    /**
     * Predicts next month's spending using linear regression on historical data,
     * per-category forecasting, and recurring transaction detection.
     * Returns null if fewer than 2 months of data are available.
     */
    fun predictNextMonth(
        trendsTxns: List<Transaction>,
        recurringTxns: List<RecurringTransaction>,
    ): MonthlyPrediction? {
        val zone = ZoneId.systemDefault()
        val debits = trendsTxns.filter { it.type == TransactionType.DEBIT }

        // Group debits by month for overall forecast
        val monthlyHistory = debits
            .groupBy { txn ->
                val date = Instant.ofEpochMilli(txn.transactionDate).atZone(zone).toLocalDate()
                YearMonth.from(date)
            }
            .entries.sortedBy { it.key }
            .mapIndexed { i, (_, txns) ->
                SpendingForecaster.MonthlySpending(i.toLong(), txns.sumOf { it.amount })
            }

        if (monthlyHistory.size < 2) return null

        val predictedTotal = forecaster.forecast(monthlyHistory)
        val confidence = forecaster.confidence(monthlyHistory)

        // Per-category forecasting
        val categoryMonthly = debits
            .groupBy { it.category ?: "OTHER" }
            .mapValues { (_, catTxns) ->
                catTxns
                    .groupBy { txn ->
                        val date = Instant.ofEpochMilli(txn.transactionDate)
                            .atZone(zone).toLocalDate()
                        YearMonth.from(date)
                    }
                    .entries.sortedBy { it.key }
                    .mapIndexed { i, (_, mTxns) ->
                        SpendingForecaster.MonthlySpending(i.toLong(), mTxns.sumOf { it.amount })
                    }
            }
        val categoryPredictions = forecaster.forecastByCategory(categoryMonthly)

        // Detect unusual items: active recurring charges not seen in recent transactions
        val unusualItems = detectUnusualItems(recurringTxns, trendsTxns)

        return MonthlyPrediction(
            predictedTotal = predictedTotal,
            categoryPredictions = categoryPredictions,
            unusualItems = unusualItems,
            confidence = confidence,
        )
    }

    /**
     * Finds recurring transactions whose merchants were NOT seen in recent transactions.
     * These are upcoming charges the user should be aware of.
     */
    private fun detectUnusualItems(
        recurring: List<RecurringTransaction>,
        recentTxns: List<Transaction>,
    ): List<String> {
        val recentMerchants = recentTxns
            .mapNotNull { it.merchant?.lowercase() }
            .toSet()
        return recurring
            .filter { it.isActive && it.merchant.lowercase() !in recentMerchants }
            .map { "${it.merchant} (~\u20B9${it.typicalAmount.toInt()})" }
            .take(3)
    }
}
