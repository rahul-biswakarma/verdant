package com.verdant.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.verdant.feature.analytics.AnalyticsScreen
import com.verdant.feature.habits.HabitsScreen
import com.verdant.feature.habits.create.CreateHabitScreen
import com.verdant.feature.habits.day.DayDetailScreen
import com.verdant.feature.habits.detail.HabitDetailScreen
import com.verdant.feature.home.HomeScreen
import com.verdant.feature.insights.InsightsScreen
import com.verdant.feature.settings.SettingsScreen
import com.verdant.feature.finance.FinanceScreen
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
    else DASHBOARD_ROUTE

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // ── Onboarding ───────────────────────────────────────
        composable(route = ONBOARDING_ROUTE) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(DASHBOARD_ROUTE) {
                        popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                    }
                },
                webClientId = webClientId,
            )
        }

        // ── Dashboard (platform hub) ─────────────────────────
        composable(route = DASHBOARD_ROUTE) {
            HomeScreen(
                onNavigateToHabits = {
                    navController.navigate(HabitsDestination.LIST.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToFinance = {
                    navController.navigate(FinanceDestination.OVERVIEW.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(SETTINGS_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHabitDetail = { id ->
                    navController.navigate("habits/detail/$id")
                },
                onCreateHabit = {
                    navController.navigate("habits/create")
                },
            )
        }

        // ── Settings ─────────────────────────────────────────
        composable(route = SETTINGS_ROUTE) {
            SettingsScreen(
                onNavigateToOnboarding = {
                    navController.navigate(ONBOARDING_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                webClientId = webClientId,
            )
        }

        // ── Habits vertical ──────────────────────────────────
        composable(route = HabitsDestination.LIST.route) {
            HabitsScreen(
                onCreateHabit = { navController.navigate("habits/create") },
                onHabitDetail = { id -> navController.navigate("habits/detail/$id") },
                onEditHabit = { navController.navigate("habits/create") },
            )
        }
        composable(route = HabitsDestination.ANALYTICS.route) {
            AnalyticsScreen()
        }
        composable(route = HabitsDestination.INSIGHTS.route) {
            InsightsScreen()
        }
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

        // ── Finance vertical ─────────────────────────────────
        composable(route = FinanceDestination.OVERVIEW.route) {
            FinanceScreen()
        }
        composable(route = FinanceDestination.TRANSACTIONS.route) {
            FinanceScreen()
        }
        composable(route = FinanceDestination.TRENDS.route) {
            FinanceScreen()
        }
    }
}
