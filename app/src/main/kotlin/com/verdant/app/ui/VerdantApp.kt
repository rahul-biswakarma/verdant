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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.verdant.app.navigation.ONBOARDING_ROUTE
import com.verdant.app.navigation.TopLevelDestination
import com.verdant.app.navigation.VerdantNavHost
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
        val currentDestination = navBackStackEntry?.destination

        // Hide bottom bar on onboarding screens
        val showBottomBar = currentDestination?.route !in listOf(ONBOARDING_ROUTE)
                && appState.onboardingCompleted == true

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    val items = TopLevelDestination.entries.map { destination ->
                        BottomBarItem(
                            icon = destination.icon,
                            label = destination.label,
                            selected = currentDestination?.hierarchy?.any {
                                it.route == destination.route
                            } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }

                    VerdantBottomBar(items = items)
                }
            },
        ) { innerPadding ->
            VerdantNavHost(
                navController = navController,
                startOnboarding = appState.onboardingCompleted == false,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
