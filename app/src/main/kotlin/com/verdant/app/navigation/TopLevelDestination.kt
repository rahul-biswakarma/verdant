package com.verdant.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.TablerIcons
import compose.icons.tablericons.ChartBar
import compose.icons.tablericons.ChartLine
import compose.icons.tablericons.Home
import compose.icons.tablericons.ListCheck
import compose.icons.tablericons.Receipt
import compose.icons.tablericons.Settings
import compose.icons.tablericons.Stars
import compose.icons.tablericons.Wallet

// ─── Dashboard (platform-level) ──────────────────────────────

/** The unified dashboard route — no bottom bar. */
const val DASHBOARD_ROUTE = "dashboard"
const val SETTINGS_ROUTE = "settings"

// ─── Habits vertical ─────────────────────────────────────────

enum class HabitsDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    LIST(route = "habits/list", label = "Habits", icon = TablerIcons.ListCheck),
    ANALYTICS(route = "habits/analytics", label = "Analytics", icon = TablerIcons.ChartBar),
    INSIGHTS(route = "habits/insights", label = "Insights", icon = TablerIcons.Stars),
}

// ─── Finance vertical ────────────────────────────────────────

enum class FinanceDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    OVERVIEW(route = "finance/overview", label = "Overview", icon = TablerIcons.Wallet),
    TRANSACTIONS(route = "finance/transactions", label = "Transactions", icon = TablerIcons.Receipt),
    TRENDS(route = "finance/trends", label = "Trends", icon = TablerIcons.ChartLine),
}

// ─── Product context detection ───────────────────────────────

/**
 * Identifies which product vertical the user is currently in,
 * based on the current navigation route.
 */
enum class ActiveProduct { HABITS, FINANCE }

fun activeProductForRoute(route: String?): ActiveProduct? = when {
    route == null -> null
    route.startsWith("habits/") -> ActiveProduct.HABITS
    route.startsWith("finance/") -> ActiveProduct.FINANCE
    else -> null // dashboard, settings, onboarding — no product-specific bottom bar
}
