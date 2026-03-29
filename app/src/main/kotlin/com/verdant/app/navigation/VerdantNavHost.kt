package com.verdant.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.verdant.feature.habits.create.CreateHabitScreen
import com.verdant.feature.habits.day.DayDetailScreen
import com.verdant.feature.habits.detail.HabitDetailScreen
import com.verdant.feature.home.HomeScreen
import com.verdant.feature.home.SummaryDashboardScreen
import com.verdant.feature.settings.SettingsScreen
import com.verdant.feature.finance.FinanceScreen
import com.verdant.feature.lifedashboard.LIFE_DASHBOARD_ROUTE
import com.verdant.feature.lifedashboard.LifeDashboardScreen
import com.verdant.feature.settings.buddies.BuddyScreen
import com.verdant.feature.settings.datasources.DataAuditScreen
import com.verdant.feature.settings.datasources.DataSourcesScreen
import com.verdant.feature.settings.devices.DeviceManagementScreen
import com.verdant.feature.settings.onboarding.OnboardingScreen

const val ONBOARDING_ROUTE = "onboarding"

@Composable
fun VerdantNavHost(
    navController: NavHostController,
    startOnboarding: Boolean,
    modifier: Modifier = Modifier,
    webClientId: String = "",
) {
    val startDestination = if (startOnboarding) ONBOARDING_ROUTE
    else HOME_ROUTE

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // ── Onboarding ───────────────────────────────────────
        composable(route = ONBOARDING_ROUTE) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(HOME_ROUTE) {
                        popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                    }
                },
                webClientId = webClientId,
            )
        }

        // ── Home (summary dashboard) ─────────────────────────
        composable(route = HOME_ROUTE) {
            SummaryDashboardScreen(
                onNavigateToSettings = {
                    navController.navigate(SETTINGS_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onNavigateToLifeDashboard = {
                    navController.navigate(LIFE_DASHBOARD_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHabitDetail = { id ->
                    navController.navigate("habits/detail/$id")
                },
                onSignedOut = {
                    navController.navigate(ONBOARDING_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Habits (container with top tabs) ─────────────────
        composable(route = HABITS_ROUTE) {
            HabitsContainerScreen(
                onCreateHabit = { navController.navigate("habits/create") },
                onHabitDetail = { id -> navController.navigate("habits/detail/$id") },
                onEditHabit = { navController.navigate("habits/create") },
            )
        }

        // ── Finance (container with internal tabs) ───────────
        composable(route = FINANCE_ROUTE) {
            FinanceScreen()
        }

        // ── Life Dashboard ──────────────────────────────────
        composable(route = LIFE_DASHBOARD_ROUTE) {
            LifeDashboardScreen()
        }

        // ── Settings ─────────────────────────────────────────
        composable(route = SETTINGS_ROUTE) {
            SettingsScreen(
                onNavigateToOnboarding = {
                    navController.navigate(ONBOARDING_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToDataSources = {
                    navController.navigate("settings/data_sources") {
                        launchSingleTop = true
                    }
                },
                onNavigateToDataAudit = {
                    navController.navigate("settings/data_audit") {
                        launchSingleTop = true
                    }
                },
                onNavigateToDevices = {
                    navController.navigate("settings/devices") {
                        launchSingleTop = true
                    }
                },
                onNavigateToBuddies = {
                    navController.navigate("settings/buddies") {
                        launchSingleTop = true
                    }
                },
                webClientId = webClientId,
            )
        }

        // ── Settings sub-screens ────────────────────────────
        composable(route = "settings/data_sources") {
            DataSourcesScreen(onBack = { navController.popBackStack() })
        }
        composable(route = "settings/data_audit") {
            DataAuditScreen(onBack = { navController.popBackStack() })
        }
        composable(route = "settings/devices") {
            DeviceManagementScreen(onBack = { navController.popBackStack() })
        }
        composable(route = "settings/buddies") {
            BuddyScreen(onBack = { navController.popBackStack() })
        }

        // ── Habit detail screens (top-level for bottom bar visibility) ──
        composable(route = "habits/create") {
            CreateHabitScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = "habits/detail/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.StringType }),
        ) {
            HabitDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDay = { date -> navController.navigate("habits/day_detail/$date") },
                onEditHabit = { navController.navigate("habits/create") },
            )
        }
        composable(
            route = "habits/day_detail/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) {
            DayDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
