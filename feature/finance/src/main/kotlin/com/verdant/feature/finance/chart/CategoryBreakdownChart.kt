package com.verdant.feature.finance.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.verdant.core.designsystem.theme.BurntOrange
import com.verdant.core.model.CategorySpend
import com.verdant.core.model.SpendingCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

@Composable
fun CategoryBreakdownChart(
    categories: List<CategorySpend>,
    modifier: Modifier = Modifier,
) {
    val displayCategories = categories
        .sortedByDescending { it.amount }
        .take(6)

    val animatables = remember(displayCategories) {
        displayCategories.map { Animatable(0f) }
    }

    LaunchedEffect(displayCategories) {
        animatables.forEachIndexed { i, anim ->
            launch {
                delay(i * 60L)
                anim.animateTo(1f, tween(400))
            }
        }
    }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val fadedOrange = BurntOrange.copy(alpha = 0.35f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        displayCategories.forEachIndexed { index, cat ->
            val barColor = lerp(fadedOrange, BurntOrange, 1f - (index * 0.12f).coerceAtMost(0.6f))
            val progress = (cat.percentage / 100f) * animatables[index].value

            val displayName = SpendingCategory.entries
                .find { it.name == cat.category }
                ?.displayName ?: cat.category

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category label
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(90.dp),
                )

                Spacer(Modifier.width(8.dp))

                // Bar
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .drawBehind {
                            // Track
                            drawRoundRect(
                                color = surfaceVariant,
                                cornerRadius = CornerRadius(4.dp.toPx()),
                                size = size,
                            )
                            // Fill
                            drawRoundRect(
                                color = barColor,
                                cornerRadius = CornerRadius(4.dp.toPx()),
                                topLeft = Offset.Zero,
                                size = Size(
                                    width = size.width * progress,
                                    height = size.height,
                                ),
                            )
                        },
                )

                Spacer(Modifier.width(8.dp))

                // Amount
                Text(
                    text = currencyFormat.format(cat.amount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(72.dp),
                )
            }
        }
    }
}
