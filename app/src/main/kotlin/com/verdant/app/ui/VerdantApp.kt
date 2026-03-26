package com.verdant.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.verdant.app.BuildConfig
import com.verdant.app.navigation.ActiveProduct
import com.verdant.app.navigation.FinanceDestination
import com.verdant.app.navigation.HabitsDestination
import com.verdant.app.navigation.ONBOARDING_ROUTE
import com.verdant.app.navigation.VerdantNavHost
import com.verdant.app.navigation.activeProductForRoute
import com.verdant.core.designsystem.component.BottomBarItem
import com.verdant.core.designsystem.component.VerdantBottomBar
import com.verdant.core.designsystem.theme.VerdantTheme

@Composable
fun VerdantApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val appState by viewModel.state.collectAsStateWithLifecycle()

    VerdantTheme(themeMode = appState.themeMode) {
        // Show splash/loading until DataStore emits the first value
        if (appState.onboardingCompleted == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@VerdantTheme
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Determine which product vertical we're in (if any)
        val activeProduct = activeProductForRoute(currentRoute)

        // Hide bottom bar on onboarding, dashboard, settings, and other non-product screens
        val showBottomBar = appState.onboardingCompleted == true
                && currentRoute !in listOf(ONBOARDING_ROUTE)
                && activeProduct != null

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar && activeProduct != null) {
                    val items = when (activeProduct) {
                        ActiveProduct.HABITS -> HabitsDestination.entries.map { dest ->
                            BottomBarItem(
                                icon = dest.icon,
                                label = dest.label,
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                        ActiveProduct.FINANCE -> FinanceDestination.entries.map { dest ->
                            BottomBarItem(
                                icon = dest.icon,
                                label = dest.label,
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    }
                    VerdantBottomBar(items = items)
                }
            },
        ) { innerPadding ->
            VerdantNavHost(
                navController = navController,
                startOnboarding = appState.onboardingCompleted == false,
                modifier = Modifier.padding(innerPadding),
                webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            )
        }
    }
}
