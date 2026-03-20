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
import com.verdant.feature.settings.onboarding.OnboardingScreen

const val ONBOARDING_ROUTE = "onboarding"

@Composable
fun VerdantNavHost(
    navController: NavHostController,
    startOnboarding: Boolean,
    modifier: Modifier = Modifier,
) {
    val startDestination = if (startOnboarding) ONBOARDING_ROUTE
    else TopLevelDestination.HOME.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // ── Onboarding ────────────────────────────────────────────────────────
        composable(route = ONBOARDING_ROUTE) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(TopLevelDestination.HOME.route) {
                        popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                    }
                },
            )
        }

        // ── Top-level destinations ────────────────────────────────────────────
        composable(route = TopLevelDestination.HOME.route) {
            HomeScreen(
                onNavigateToHabitDetail = { id -> navController.navigate("habit_detail/$id") },
            )
        }
        composable(route = TopLevelDestination.HABITS.route) {
            HabitsScreen(
                onCreateHabit = { navController.navigate("create_habit") },
                onHabitDetail = { id -> navController.navigate("habit_detail/$id") },
                onEditHabit = { id -> navController.navigate("create_habit") },
            )
        }
        composable(route = TopLevelDestination.ANALYTICS.route) { AnalyticsScreen() }
        composable(route = TopLevelDestination.INSIGHTS.route) { InsightsScreen() }
        composable(route = TopLevelDestination.SETTINGS.route) {
            SettingsScreen(
                onNavigateToOnboarding = {
                    navController.navigate(ONBOARDING_ROUTE) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Habit creation ────────────────────────────────────────────────────
        composable(route = "create_habit") {
            CreateHabitScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Habit detail ──────────────────────────────────────────────────────
        composable(
            route = "habit_detail/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.StringType }),
        ) {
            HabitDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDay = { date -> navController.navigate("day_detail/$date") },
                onEditHabit = { navController.navigate("create_habit") },
            )
        }

        // ── Day detail ────────────────────────────────────────────────────────
        composable(
            route = "day_detail/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) {
            DayDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
