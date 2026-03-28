package com.verdant.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.TablerIcons
import compose.icons.tablericons.ChartBar
import compose.icons.tablericons.ChartLine
import compose.icons.tablericons.Home
import compose.icons.tablericons.ListCheck
import compose.icons.tablericons.Receipt
import compose.icons.tablericons.Stars
import compose.icons.tablericons.Wallet

// ─── Routes ──────────────────────────────────────────────────

const val HOME_ROUTE = "home"
const val HABITS_ROUTE = "habits"
const val FINANCE_ROUTE = "finance"
const val SETTINGS_ROUTE = "settings"

// ─── Global bottom bar tabs ─────────────────────────────────

enum class GlobalTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(route = HOME_ROUTE, label = "Home", icon = TablerIcons.Home),
    HABITS(route = HABITS_ROUTE, label = "Habits", icon = TablerIcons.ListCheck),
    FINANCE(route = FINANCE_ROUTE, label = "Finance", icon = TablerIcons.Wallet),
}

fun globalTabForRoute(route: String?): GlobalTab? = when {
    route == null -> null
    route == HOME_ROUTE || route == SETTINGS_ROUTE -> GlobalTab.HOME
    route.startsWith("habits") -> GlobalTab.HABITS
    route.startsWith("finance") -> GlobalTab.FINANCE
    else -> null
}

// ─── Habits secondary tabs ──────────────────────────────────

enum class HabitsDestination(
    val label: String,
    val icon: ImageVector,
) {
    LIST(label = "Habits", icon = TablerIcons.ListCheck),
    ANALYTICS(label = "Analytics", icon = TablerIcons.ChartBar),
    INSIGHTS(label = "Insights", icon = TablerIcons.Stars),
}

// ─── Finance secondary tabs ─────────────────────────────────

enum class FinanceDestination(
    val label: String,
    val icon: ImageVector,
) {
    OVERVIEW(label = "Overview", icon = TablerIcons.Wallet),
    TRANSACTIONS(label = "Transactions", icon = TablerIcons.Receipt),
    TRENDS(label = "Trends", icon = TablerIcons.ChartLine),
}
