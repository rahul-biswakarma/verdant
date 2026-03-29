package com.verdant.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import com.verdant.app.navigation.GlobalTab
import com.verdant.app.navigation.HOME_ROUTE
import com.verdant.app.navigation.ONBOARDING_ROUTE
import com.verdant.app.navigation.VerdantNavHost
import com.verdant.app.navigation.globalTabForRoute
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

        val activeTab = globalTabForRoute(currentRoute)
        val showBottomBar = appState.onboardingCompleted == true
                && currentRoute != ONBOARDING_ROUTE

        Box(modifier = Modifier.fillMaxSize()) {
            VerdantNavHost(
                navController = navController,
                startOnboarding = appState.onboardingCompleted == false,
                webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            )

            if (showBottomBar) {
                val items = GlobalTab.entries.map { tab ->
                    BottomBarItem(
                        icon = tab.icon,
                        label = tab.label,
                        selected = activeTab == tab,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                VerdantBottomBar(
                    items = items,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
