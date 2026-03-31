package com.verdant.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.TablerIcons
import compose.icons.tablericons.CalendarEvent
import compose.icons.tablericons.ChartBar
import compose.icons.tablericons.ChartLine
import compose.icons.tablericons.Home
import compose.icons.tablericons.ListCheck
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Receipt
import compose.icons.tablericons.Wallet

// ─── Routes ──────────────────────────────────────────────────

const val HOME_ROUTE = "home"
const val HABITS_ROUTE = "habits"
const val STORIES_ROUTE = "stories"
const val FINANCE_ROUTE = "finance"
const val SETTINGS_ROUTE = "settings"

// ─── Global bottom bar tabs (shown on Home) ────────────────

enum class GlobalTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(route = HOME_ROUTE, label = "Home", icon = TablerIcons.Home),
    HABITS(route = HABITS_ROUTE, label = "Habits", icon = TablerIcons.ListCheck),
    STORIES(route = STORIES_ROUTE, label = "Stories", icon = TablerIcons.CalendarEvent),
    FINANCE(route = FINANCE_ROUTE, label = "Finance", icon = TablerIcons.Wallet),
}

// ─── Habits bottom bar tabs ────────────────────────────────

enum class HabitsTab(
    val label: String,
    val icon: ImageVector,
) {
    LIST(label = "Habits", icon = TablerIcons.ListCheck),
    ANALYTICS(label = "Analytics", icon = TablerIcons.ChartBar),
    CREATE(label = "Create", icon = TablerIcons.Plus),
}

// ─── Finance bottom bar tabs ───────────────────────────────

enum class FinanceNavTab(
    val label: String,
    val icon: ImageVector,
) {
    OVERVIEW(label = "Overview", icon = TablerIcons.Wallet),
    TRANSACTIONS(label = "Transactions", icon = TablerIcons.Receipt),
    TRENDS(label = "Trends", icon = TablerIcons.ChartLine),
    CREATE(label = "Add", icon = TablerIcons.Plus),
}

// ─── Stories bottom bar tabs ───────────────────────────────

enum class StoriesTab(
    val label: String,
    val icon: ImageVector,
) {
    LIST(label = "Stories", icon = TablerIcons.CalendarEvent),
    CREATE(label = "Create", icon = TablerIcons.Plus),
}

// Keep for backward compat with secondary tab references
typealias HabitsDestination = HabitsTab

// ─── Navigation context resolver ───────────────────────────

/**
 * Determines which bottom bar context the current route belongs to.
 */
enum class NavContext { HOME, HABITS, FINANCE, STORIES }

fun navContextForRoute(route: String?): NavContext? = when {
    route == null -> null
    route == HOME_ROUTE -> NavContext.HOME
    route == HABITS_ROUTE -> NavContext.HABITS
    route == FINANCE_ROUTE || route == "finance/transaction/create" -> NavContext.FINANCE
    route == STORIES_ROUTE || route == "stories/create" -> NavContext.STORIES
    else -> null // Detail screens → no bottom bar
}

/** Legacy helper — maps route to GlobalTab (used only on home context). */
fun globalTabForRoute(route: String?): GlobalTab? = when {
    route == null -> null
    route == HOME_ROUTE || route == SETTINGS_ROUTE -> GlobalTab.HOME
    route.startsWith("habits") -> GlobalTab.HABITS
    route.startsWith("stories") -> GlobalTab.STORIES
    route.startsWith("finance") -> GlobalTab.FINANCE
    else -> null
}
