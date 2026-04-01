package com.verdant.feature.home.genui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.genui.data.DashboardDataResolver
import com.verdant.core.genui.model.ComponentType
import com.verdant.core.genui.model.DashboardLayout
import com.verdant.core.genui.model.DashboardSection
import com.verdant.core.genui.model.DisplayCondition
import com.verdant.core.genui.model.ResolvedData
import com.verdant.core.genui.model.SpanType

/**
 * Renders a [DashboardLayout] dynamically. Each section is resolved
 * against live repository data and rendered by the matching composable.
 *
 * The header (greeting, avatar, settings) is rendered externally by the
 * caller; this composable only renders the dynamic content area.
 */
@Composable
fun DynamicDashboard(
    layout: DashboardLayout,
    dataResolver: DashboardDataResolver,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = layout.sections.sortedByDescending { it.priority }
    val groups = groupForLayout(sections)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        groups.forEach { group ->
            when (group) {
                is LayoutGroup.Single -> {
                    RenderSection(
                        section = group.section,
                        dataResolver = dataResolver,
                        onNavigate = onNavigate,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                is LayoutGroup.HalfPair -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RenderSection(
                            section = group.left,
                            dataResolver = dataResolver,
                            onNavigate = onNavigate,
                            modifier = Modifier.weight(1f),
                        )
                        RenderSection(
                            section = group.right,
                            dataResolver = dataResolver,
                            onNavigate = onNavigate,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // bottom bar clearance
    }
}

@Composable
private fun RenderSection(
    section: DashboardSection,
    dataResolver: DashboardDataResolver,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val data by dataResolver.observeResolved(section.dataSource)
        .collectAsStateWithLifecycle(initialValue = ResolvedData())

    // Evaluate display condition
    val condition = section.condition
    if (condition != null && !evaluateCondition(condition, data)) return

    // Handle compound types
    when (section.component) {
        ComponentType.STAT_ROW -> {
            val children = section.config.children ?: return
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                children.forEach { child ->
                    RenderSection(
                        section = child,
                        dataResolver = dataResolver,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            return
        }
        ComponentType.CATEGORY_GRID -> {
            val children = section.config.children ?: return
            val pairs = children.chunked(2)
            Column(modifier = modifier) {
                pairs.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEach { child ->
                            RenderSection(
                                section = child,
                                dataResolver = dataResolver,
                                onNavigate = onNavigate,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Pad if odd number
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            return
        }
        else -> { /* Render as leaf component below */ }
    }

    // Render leaf component
    when (section.component) {
        ComponentType.TODAY_HABITS -> RenderTodayHabits(section.config, data, onNavigate, modifier)
        ComponentType.PROGRESS_RING -> RenderProgressRing(section.config, data, onNavigate, modifier)
        ComponentType.CATEGORY_NAV -> RenderCategoryNav(section.config, data, onNavigate, modifier)
        ComponentType.STAT_CARD -> RenderStatCard(section.config, data, onNavigate, modifier)
        ComponentType.FINANCE_SUMMARY -> RenderFinanceSummary(section.config, data, onNavigate, modifier)
        ComponentType.LIFE_DASHBOARD -> RenderLifeDashboard(section.config, data, onNavigate, modifier)
        ComponentType.INSIGHT_TEXT -> RenderInsightText(section.config, data, onNavigate, modifier)
        ComponentType.STREAK_HIGHLIGHT -> RenderStreakHighlight(section.config, data, onNavigate, modifier)
        ComponentType.AI_PREDICTIONS -> RenderAIPredictions(section.config, data, onNavigate, modifier)
        ComponentType.ACTIVITY_RECAP -> RenderActivityRecap(section.config, data, onNavigate, modifier)
        ComponentType.MOOD_SUMMARY -> RenderInsightText(section.config, data, onNavigate, modifier) // reuse
        ComponentType.HEALTH_SNAPSHOT -> RenderInsightText(section.config, data, onNavigate, modifier) // reuse
        ComponentType.CONTRIBUTION_GRID -> { /* TODO: integrate HabitContributionGrid */ }
        ComponentType.STAT_ROW,
        ComponentType.CATEGORY_GRID -> { /* Already handled above */ }
    }
}

// ── Layout grouping ───────────────────────────────────────────────

private sealed class LayoutGroup {
    data class Single(val section: DashboardSection) : LayoutGroup()
    data class HalfPair(val left: DashboardSection, val right: DashboardSection) : LayoutGroup()
}

private fun groupForLayout(sections: List<DashboardSection>): List<LayoutGroup> {
    val result = mutableListOf<LayoutGroup>()
    var i = 0
    while (i < sections.size) {
        val section = sections[i]
        if (section.span == SpanType.HALF && i + 1 < sections.size && sections[i + 1].span == SpanType.HALF) {
            result.add(LayoutGroup.HalfPair(section, sections[i + 1]))
            i += 2
        } else {
            result.add(LayoutGroup.Single(section))
            i++
        }
    }
    return result
}

// ── Condition evaluation ──────────────────────────────────────────

private fun evaluateCondition(condition: DisplayCondition, data: ResolvedData): Boolean {
    val fieldValue = data.floatOrNull(condition.field)
        ?: data.intOrNull(condition.field)?.toFloat()
        ?: return true // If field not found, show the section

    val threshold = condition.value.toFloatOrNull() ?: return true

    return when (condition.operator) {
        "gt" -> fieldValue > threshold
        "gte" -> fieldValue >= threshold
        "lt" -> fieldValue < threshold
        "lte" -> fieldValue <= threshold
        "eq" -> fieldValue == threshold
        "ne" -> fieldValue != threshold
        else -> true
    }
}
