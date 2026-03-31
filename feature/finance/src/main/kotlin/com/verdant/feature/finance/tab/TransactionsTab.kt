package com.verdant.feature.finance.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verdant.core.model.SpendingCategory
import com.verdant.core.model.Transaction
import com.verdant.core.model.TransactionType
import com.verdant.feature.finance.TransactionFilters
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowDownRight
import compose.icons.tablericons.ArrowUpRight
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Search
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

@Composable
fun TransactionsTab(
    transactions: List<Transaction>,
    searchQuery: String,
    activeFilters: TransactionFilters,
    onTransactionClick: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFiltersChange: (TransactionFilters) -> Unit,
    onCreateTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search transactions") },
                leadingIcon = {
                    Icon(
                        imageVector = TablerIcons.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Type filter chips
                FilterChip(
                    selected = activeFilters.type == null,
                    onClick = { onFiltersChange(activeFilters.copy(type = null)) },
                    label = { Text("All") },
                )
                FilterChip(
                    selected = activeFilters.type == TransactionType.DEBIT,
                    onClick = {
                        onFiltersChange(
                            activeFilters.copy(
                                type = if (activeFilters.type == TransactionType.DEBIT) null
                                else TransactionType.DEBIT,
                            ),
                        )
                    },
                    label = { Text("Expenses") },
                )
                FilterChip(
                    selected = activeFilters.type == TransactionType.CREDIT,
                    onClick = {
                        onFiltersChange(
                            activeFilters.copy(
                                type = if (activeFilters.type == TransactionType.CREDIT) null
                                else TransactionType.CREDIT,
                            ),
                        )
                    },
                    label = { Text("Income") },
                )

                Spacer(Modifier.width(4.dp))

                // Category filter chips
                val topCategories = listOf(
                    SpendingCategory.FOOD,
                    SpendingCategory.SHOPPING,
                    SpendingCategory.BILLS,
                    SpendingCategory.TRANSPORT,
                    SpendingCategory.ENTERTAINMENT,
                )
                topCategories.forEach { cat ->
                    FilterChip(
                        selected = activeFilters.category == cat.name,
                        onClick = {
                            onFiltersChange(
                                activeFilters.copy(
                                    category = if (activeFilters.category == cat.name) null
                                    else cat.name,
                                ),
                            )
                        },
                        label = { Text(cat.displayName) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Transaction list
            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank() || activeFilters.type != null || activeFilters.category != null) {
                            "No transactions match your filters"
                        } else {
                            "No transactions yet"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    items(transactions, key = { it.id }) { txn ->
                        TransactionRow(
                            transaction = txn,
                            onClick = { onTransactionClick(txn.id) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
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
