package com.verdant.core.genui.generation

import com.verdant.core.genui.model.ComponentConfig
import com.verdant.core.genui.model.ComponentType
import com.verdant.core.genui.model.DashboardLayout
import com.verdant.core.genui.model.DashboardSection
import com.verdant.core.genui.model.DataSource
import com.verdant.core.genui.model.DataSourceRef
import com.verdant.core.genui.model.SpanType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides a static [DashboardLayout] that mirrors the current hardcoded
 * [SummaryDashboardScreen] layout. Used as the last-resort fallback when
 * both LLM generation and cached layouts are unavailable.
 */
@Singleton
class FallbackLayoutProvider @Inject constructor() {

    fun defaultLayout(): DashboardLayout = DashboardLayout(
        version = 1,
        generatedAt = System.currentTimeMillis(),
        expiresAt = Long.MAX_VALUE,
        sections = listOf(
            // Today's Habits Summary Card (rose)
            DashboardSection(
                id = "today_habits",
                component = ComponentType.TODAY_HABITS,
                config = ComponentConfig(backgroundColor = "#F8E1E4"),
                dataSource = DataSourceRef(source = DataSource.HABITS_TODAY),
                priority = 100,
            ),
            // Your Progress Card (lavender)
            DashboardSection(
                id = "progress",
                component = ComponentType.PROGRESS_RING,
                config = ComponentConfig(backgroundColor = "#E4D9F5"),
                dataSource = DataSourceRef(source = DataSource.HABITS_COMPLETION),
                priority = 90,
            ),
            // Category Cards (2x2 grid)
            DashboardSection(
                id = "categories",
                component = ComponentType.CATEGORY_GRID,
                config = ComponentConfig(
                    children = listOf(
                        DashboardSection(
                            id = "cat_habits",
                            component = ComponentType.CATEGORY_NAV,
                            span = SpanType.HALF,
                            config = ComponentConfig(
                                title = "Habits",
                                subtitle = "Stay Consistent",
                                icon = "ListCheck",
                                backgroundColor = "#D4EBD9",
                                iconColor = "#4CAF72",
                            ),
                            dataSource = DataSourceRef(source = DataSource.STATIC),
                        ),
                        DashboardSection(
                            id = "cat_wellness",
                            component = ComponentType.CATEGORY_NAV,
                            span = SpanType.HALF,
                            config = ComponentConfig(
                                title = "Wellness",
                                subtitle = "Mind & Body",
                                icon = "Heart",
                                backgroundColor = "#E4D9F5",
                                iconColor = "#8B6FC0",
                            ),
                            dataSource = DataSourceRef(source = DataSource.STATIC),
                        ),
                        DashboardSection(
                            id = "cat_finance",
                            component = ComponentType.CATEGORY_NAV,
                            span = SpanType.HALF,
                            config = ComponentConfig(
                                title = "Finance",
                                subtitle = "Track Spending",
                                icon = "Wallet",
                                backgroundColor = "#FDE5D0",
                                iconColor = "#E8864A",
                            ),
                            dataSource = DataSourceRef(source = DataSource.STATIC),
                        ),
                        DashboardSection(
                            id = "cat_insights",
                            component = ComponentType.CATEGORY_NAV,
                            span = SpanType.HALF,
                            config = ComponentConfig(
                                title = "Insights",
                                subtitle = "AI Analysis",
                                icon = "Stars",
                                backgroundColor = "#F5D0D5",
                                iconColor = "#C94C60",
                            ),
                            dataSource = DataSourceRef(source = DataSource.STATIC),
                        ),
                    ),
                ),
                dataSource = DataSourceRef(source = DataSource.STATIC),
                priority = 80,
            ),
            // Activity Recap
            DashboardSection(
                id = "activity_recap",
                component = ComponentType.ACTIVITY_RECAP,
                dataSource = DataSourceRef(
                    source = DataSource.HABITS_TODAY,
                    filters = mapOf("maxItems" to "3"),
                ),
                priority = 70,
            ),
            // Stats Row (Best Streak + Today's Score)
            DashboardSection(
                id = "stats_row",
                component = ComponentType.STAT_ROW,
                config = ComponentConfig(
                    children = listOf(
                        DashboardSection(
                            id = "stat_streak",
                            component = ComponentType.STAT_CARD,
                            span = SpanType.HALF,
                            config = ComponentConfig(
                                statLabel = "Best Streak",
                                statUnit = "days",
                                icon = "Flame",
                                backgroundColor = "#E4D9F5",
                                iconColor = "#8B6FC0",
                            ),
                            dataSource = DataSourceRef(source = DataSource.STREAKS),
                        ),
                        DashboardSection(
                            id = "stat_score",
                            component = ComponentType.STAT_CARD,
                            span = SpanType.HALF,
                            config = ComponentConfig(
                                statLabel = "Today's Score",
                                statUnit = "",
                                icon = "Trophy",
                                backgroundColor = "#F5D0D5",
                                iconColor = "#C94C60",
                            ),
                            dataSource = DataSourceRef(source = DataSource.HABITS_COMPLETION),
                        ),
                    ),
                ),
                dataSource = DataSourceRef(source = DataSource.STATIC),
                priority = 60,
            ),
            // Finance Card
            DashboardSection(
                id = "finance",
                component = ComponentType.FINANCE_SUMMARY,
                config = ComponentConfig(backgroundColor = "#F8E1E4"),
                dataSource = DataSourceRef(source = DataSource.TRANSACTIONS_MONTHLY),
                priority = 50,
            ),
            // AI Predictions
            DashboardSection(
                id = "predictions",
                component = ComponentType.AI_PREDICTIONS,
                config = ComponentConfig(showRefreshButton = true),
                dataSource = DataSourceRef(source = DataSource.PREDICTIONS_ACTIVE),
                priority = 40,
            ),
            // Life Dashboard
            DashboardSection(
                id = "life_dashboard",
                component = ComponentType.LIFE_DASHBOARD,
                config = ComponentConfig(navigateTo = "life_dashboard"),
                dataSource = DataSourceRef(source = DataSource.PLAYER_PROFILE),
                priority = 30,
            ),
        ),
    )
}
