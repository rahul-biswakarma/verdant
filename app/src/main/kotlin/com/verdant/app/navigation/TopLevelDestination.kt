package com.verdant.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(
        route = "home",
        label = "Home",
        icon = Icons.Filled.Home,
    ),
    HABITS(
        route = "habits",
        label = "Habits",
        icon = Icons.Filled.CheckBox,
    ),
    ANALYTICS(
        route = "analytics",
        label = "Analytics",
        icon = Icons.Filled.Analytics,
    ),
    INSIGHTS(
        route = "insights",
        label = "Insights",
        icon = Icons.Filled.AutoAwesome,
    ),
    SETTINGS(
        route = "settings",
        label = "Settings",
        icon = Icons.Filled.Settings,
    ),
}
