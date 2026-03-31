package com.verdant.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.verdant.app.BuildConfig
import com.verdant.app.navigation.FinanceNavTab
import com.verdant.app.navigation.GlobalTab
import com.verdant.app.navigation.HOME_ROUTE
import com.verdant.app.navigation.HabitsTab
import com.verdant.app.navigation.NavContext
import com.verdant.app.navigation.ONBOARDING_ROUTE
import com.verdant.app.navigation.StoriesTab
import com.verdant.app.navigation.VerdantNavHost
import com.verdant.app.navigation.navContextForRoute
import com.verdant.core.designsystem.component.BottomBarItem
import com.verdant.core.designsystem.component.VerdantBottomBar
import com.verdant.core.designsystem.theme.VerdantTheme

@Composable
fun VerdantApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val appState by viewModel.state.collectAsStateWithLifecycle()

    VerdantTheme(themeMode = appState.themeMode) {
        if (appState.onboardingCompleted == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@VerdantTheme
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val navContext = navContextForRoute(currentRoute)
        val showBottomBar = appState.onboardingCompleted == true
                && currentRoute != ONBOARDING_ROUTE
                && navContext != null

        // Track selected sub-tabs for each section
        var habitsTab by rememberSaveable { mutableStateOf(HabitsTab.LIST) }
        var financeTab by rememberSaveable { mutableStateOf(FinanceNavTab.OVERVIEW) }
        var storiesTab by rememberSaveable { mutableStateOf(StoriesTab.LIST) }

        Box(modifier = Modifier.fillMaxSize()) {
            VerdantNavHost(
                navController = navController,
                startOnboarding = appState.onboardingCompleted == false,
                webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                isDebugBuild = BuildConfig.DEBUG,
                habitsTab = habitsTab,
                onHabitsBackFromCreate = { habitsTab = HabitsTab.LIST },
                financeTab = financeTab,
                storiesTab = storiesTab,
            )

            if (showBottomBar && navContext != null) {
                val items = when (navContext) {
                    NavContext.HOME -> GlobalTab.entries.map { tab ->
                        BottomBarItem(
                            icon = tab.icon,
                            label = tab.label,
                            selected = tab == GlobalTab.HOME,
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

                    NavContext.HABITS -> HabitsTab.entries.map { tab ->
                        BottomBarItem(
                            icon = tab.icon,
                            label = tab.label,
                            selected = habitsTab == tab,
                            onClick = { habitsTab = tab },
                        )
                    }

                    NavContext.FINANCE -> FinanceNavTab.entries.map { tab ->
                        BottomBarItem(
                            icon = tab.icon,
                            label = tab.label,
                            selected = financeTab == tab,
                            onClick = {
                                if (tab == FinanceNavTab.CREATE) {
                                    navController.navigate("finance/transaction/create")
                                } else {
                                    financeTab = tab
                                }
                            },
                        )
                    }

                    NavContext.STORIES -> StoriesTab.entries.map { tab ->
                        BottomBarItem(
                            icon = tab.icon,
                            label = tab.label,
                            selected = storiesTab == tab,
                            onClick = {
                                if (tab == StoriesTab.CREATE) {
                                    navController.navigate("stories/create")
                                } else {
                                    storiesTab = tab
                                }
                            },
                        )
                    }

                }

                VerdantBottomBar(
                    items = items,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
