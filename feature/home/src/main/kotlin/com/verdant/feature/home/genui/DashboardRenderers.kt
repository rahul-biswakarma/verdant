package com.verdant.feature.home.genui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdant.core.designsystem.component.CompletionRing
import com.verdant.core.genui.model.ComponentConfig
import com.verdant.core.genui.model.ResolvedData
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowRight
import compose.icons.tablericons.ChartBar
import compose.icons.tablericons.CurrencyRupee
import compose.icons.tablericons.Flame
import compose.icons.tablericons.Heart
import compose.icons.tablericons.ListCheck
import compose.icons.tablericons.Refresh
import compose.icons.tablericons.Stars
import compose.icons.tablericons.Target
import compose.icons.tablericons.Trophy
import compose.icons.tablericons.Wallet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Color helpers ──────────────────────────────────────────────────

private fun parseColor(hex: String?, fallback: Color): Color =
    hex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (_: Exception) {
            fallback
        }
    } ?: fallback

private fun resolveIcon(name: String?) = when (name) {
    "ListCheck" -> TablerIcons.ListCheck
    "Heart" -> TablerIcons.Heart
    "Wallet" -> TablerIcons.Wallet
    "Stars" -> TablerIcons.Stars
    "Flame" -> TablerIcons.Flame
    "Trophy" -> TablerIcons.Trophy
    "ChartBar" -> TablerIcons.ChartBar
    "Target" -> TablerIcons.Target
    "CurrencyRupee" -> TablerIcons.CurrencyRupee
    else -> TablerIcons.Stars
}

private val TextDark = Color(0xFF2E2D2B)
private val TextMuted = Color(0xFF5C5A57)

// ── TODAY_HABITS ───────────────────────────────────────────────────

@Composable
fun RenderTodayHabits(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = parseColor(config.backgroundColor, Color(0xFFF8E1E4))
    val iconColor = parseColor(config.iconColor, Color(0xFFD4626E))
    val completed = data.intOrNull("completedHabits") ?: 0
    val total = data.intOrNull("totalHabits") ?: 0
    val remaining = data.intOrNull("remainingHabits") ?: (total - completed)
    val percent = data.floatOrNull("completionPercent") ?: 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = TablerIcons.ListCheck,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = config.title ?: "Today's Habits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                    )
                    Text(
                        text = "$completed of $total completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GenStatPill("Done", "$completed")
                GenStatPill("Remaining", "$remaining")
                GenStatPill("Score", "${(percent * 100).toInt()}%")
            }
        }
    }
}

@Composable
private fun GenStatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextDark,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
    }
}

// ── PROGRESS_RING ─────────────────────────────────────────────────

@Composable
fun RenderProgressRing(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = parseColor(config.backgroundColor, Color(0xFFE4D9F5))
    val ringColor = parseColor(config.iconColor, Color(0xFF8B6FC0))
    val percent = data.floatOrNull("completionPercent") ?: 0f
    val completed = data.intOrNull("completed") ?: 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ringColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = TablerIcons.ChartBar,
                            contentDescription = null,
                            tint = ringColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = config.title ?: "Your Progress",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(percent * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                )
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM")),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CompletionRing(
                    progress = percent,
                    color = ringColor,
                    size = 80.dp,
                    strokeWidth = 8.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$completed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                    )
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

// ── CATEGORY_NAV ──────────────────────────────────────────────────

@Composable
fun RenderCategoryNav(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = parseColor(config.backgroundColor, Color(0xFFD4EBD9))
    val iconColor = parseColor(config.iconColor, Color(0xFF4CAF72))
    val icon = resolveIcon(config.icon)

    Card(
        modifier = modifier
            .aspectRatio(1.1f)
            .clickable { config.navigateTo?.let(onNavigate) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = config.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text = config.subtitle ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Check",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = TablerIcons.ArrowRight,
                        contentDescription = null,
                        tint = TextDark,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// ── STAT_CARD ─────────────────────────────────────────────────────

@Composable
fun RenderStatCard(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = parseColor(config.backgroundColor, Color(0xFFE4D9F5))
    val iconColor = parseColor(config.iconColor, Color(0xFF8B6FC0))
    val icon = resolveIcon(config.icon)
    val label = config.statLabel ?: ""
    val unit = config.statUnit ?: ""

    // Try to resolve value from data
    val value = when {
        data.intOrNull("bestStreak") != null && label.contains("Streak", ignoreCase = true) ->
            "${data.intOrNull("bestStreak")}"
        data.floatOrNull("completionPercent") != null && label.contains("Score", ignoreCase = true) ->
            "${((data.floatOrNull("completionPercent") ?: 0f) * 100).toInt()}%"
        else -> data.stringOrNull("value") ?: "0"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}

// ── FINANCE_SUMMARY ───────────────────────────────────────────────

@Composable
fun RenderFinanceSummary(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = parseColor(config.backgroundColor, Color(0xFFF8E1E4))
    val spent = data.floatOrNull("monthlySpent")?.toDouble()
        ?: data.stringOrNull("monthlySpent")?.toDoubleOrNull() ?: 0.0
    val income = data.floatOrNull("monthlyIncome")?.toDouble()
        ?: data.stringOrNull("monthlyIncome")?.toDoubleOrNull() ?: 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFD4626E).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = TablerIcons.Wallet,
                        contentDescription = null,
                        tint = Color(0xFFD4626E),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = config.title ?: "Finance This Month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Spent", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = "₹%.0f".format(spent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC94C60),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Income", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = "₹%.0f".format(income),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF72),
                    )
                }
            }
        }
    }
}

// ── LIFE_DASHBOARD ────────────────────────────────────────────────

@Composable
fun RenderLifeDashboard(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = parseColor(config.backgroundColor, Color(0xFFD6E8F8))
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { config.navigateTo?.let(onNavigate) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4A90D9).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TablerIcons.Trophy,
                    contentDescription = null,
                    tint = Color(0xFF4A90D9),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.title ?: "Life Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                )
                Text(
                    text = config.subtitle ?: "Quests, XP & player profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            Icon(
                imageVector = TablerIcons.ArrowRight,
                contentDescription = null,
                tint = TextDark,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── INSIGHT_TEXT ───────────────────────────────────────────────────

@Composable
fun RenderInsightText(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = parseColor(config.backgroundColor, Color(0xFFF5D0D5))
    val text = config.insightText ?: data.stringOrNull("latestContent") ?: ""
    if (text.isBlank()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = TablerIcons.Stars,
                contentDescription = null,
                tint = Color(0xFFC94C60),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── STREAK_HIGHLIGHT ──────────────────────────────────────────────

@Composable
fun RenderStreakHighlight(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val streak = data.intOrNull("bestStreak") ?: 0
    if (streak == 0) return

    val bg = parseColor(config.backgroundColor, Color(0xFFFDE5D0))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = TablerIcons.Flame,
                contentDescription = null,
                tint = Color(0xFFE8864A),
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = config.title ?: "Streak On Fire!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                )
                Text(
                    text = "$streak day streak going strong",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
        }
    }
}

// ── AI_PREDICTIONS ────────────────────────────────────────────────

@Composable
fun RenderAIPredictions(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val predictions = data.listOrNull("predictions") ?: emptyList()
    if (predictions.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE4F7)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF7C4DFF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = TablerIcons.Stars,
                            contentDescription = null,
                            tint = Color(0xFF7C4DFF),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = config.title ?: "AI Predictions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            predictions.forEachIndexed { index, element ->
                val obj = element as? kotlinx.serialization.json.JsonObject ?: return@forEachIndexed
                val type = (obj["type"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                val predData = (obj["data"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                val confidence = (obj["confidence"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 0f
                val (predIcon, predLabel) = when (type) {
                    "SPENDING_FORECAST" -> TablerIcons.CurrencyRupee to "Spending"
                    "HABIT_SUSTAINABILITY" -> TablerIcons.Target to "Habits"
                    "HEALTH_TRAJECTORY" -> TablerIcons.Heart to "Health"
                    else -> TablerIcons.Stars to "Life"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(predIcon, null, tint = Color(0xFF7C4DFF).copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(predLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = TextMuted)
                        Text(predData.lines().firstOrNull() ?: predData, style = MaterialTheme.typography.bodySmall, color = TextDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${(confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                if (index < predictions.lastIndex) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── ACTIVITY_RECAP ────────────────────────────────────────────────

@Composable
fun RenderActivityRecap(
    config: ComponentConfig,
    data: ResolvedData,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = config.title ?: "My Activity Recap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            val habits = data.listOrNull("topHabits")
            if (habits.isNullOrEmpty()) {
                Text(
                    text = "No habits tracked yet. Start building your routine!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                habits.take(config.maxItems ?: 3).forEach { habitElement ->
                    val obj = try {
                        habitElement as? kotlinx.serialization.json.JsonObject
                    } catch (_: Exception) { null }
                    if (obj != null) {
                        val name = obj["name"]?.let {
                            (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                        } ?: ""
                        val icon = obj["icon"]?.let {
                            (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                        } ?: ""
                        val completed = obj["completed"]?.let {
                            (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull()
                        } ?: false

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = icon, style = MaterialTheme.typography.bodyLarge)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (completed) Color(0xFFD4EBD9) else Color(0xFFFDE5D0),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = if (completed) "Done" else "Pending",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (completed) Color(0xFF4CAF72) else Color(0xFFE8864A),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
