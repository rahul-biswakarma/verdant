package com.verdant.core.common

import com.verdant.core.model.CategorySpend
import com.verdant.core.model.MerchantSpend
import com.verdant.core.model.MonthlySpendingSummary
import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceDataAggregator @Inject constructor() {

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
}
