package com.verdant.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.TablerIcons
import compose.icons.tablericons.ChartBar
import compose.icons.tablericons.Home
import compose.icons.tablericons.ListCheck
import compose.icons.tablericons.Settings
import compose.icons.tablericons.Stars

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(
        route = "home",
        label = "Home",
        icon = TablerIcons.Home,
    ),
    HABITS(
        route = "habits",
        label = "Habits",
        icon = TablerIcons.ListCheck,
    ),
    ANALYTICS(
        route = "analytics",
        label = "Analytics",
        icon = TablerIcons.ChartBar,
    ),
    INSIGHTS(
        route = "insights",
        label = "Insights",
        icon = TablerIcons.Stars,
    ),
    SETTINGS(
        route = "settings",
        label = "Settings",
        icon = TablerIcons.Settings,
    ),
}
