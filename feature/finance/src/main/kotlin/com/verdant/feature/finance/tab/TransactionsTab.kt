package com.verdant.feature.finance.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowDownRight
import compose.icons.tablericons.ArrowUpRight
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

@Composable
fun TransactionsTab(
    transactions: List<Transaction>,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (transactions.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(transactions, key = { it.id }) { txn ->
            TransactionRow(
                transaction = txn,
                onClick = { onTransactionClick(txn.id) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
) {
    val isDebit = transaction.type == TransactionType.DEBIT
    val amountColor = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val sign = if (isDebit) "-" else "+"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (isDebit) TablerIcons.ArrowUpRight else TablerIcons.ArrowDownRight,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = amountColor,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant ?: transaction.category ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = buildString {
                    transaction.category?.let { append(it) }
                    append(" \u00B7 ")
                    append(dateFormat.format(Date(transaction.transactionDate)))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        Text(
            text = "$sign${currencyFormat.format(transaction.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
    }
}
