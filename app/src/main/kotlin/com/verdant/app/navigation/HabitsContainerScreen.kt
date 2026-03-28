package com.verdant.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.verdant.feature.analytics.AnalyticsScreen
import com.verdant.feature.habits.HabitsScreen
import com.verdant.feature.insights.InsightsScreen

@Composable
fun HabitsContainerScreen(
    onCreateHabit: () -> Unit,
    onHabitDetail: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = HabitsDestination.entries

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        ) {
            tabs.forEachIndexed { index, dest ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(dest.label) },
                    icon = { Icon(imageVector = dest.icon, contentDescription = dest.label) },
                )
            }
        }

        when (tabs[selectedTab]) {
            HabitsDestination.LIST -> HabitsScreen(
                onCreateHabit = onCreateHabit,
                onHabitDetail = onHabitDetail,
                onEditHabit = onEditHabit,
            )
            HabitsDestination.ANALYTICS -> AnalyticsScreen()
            HabitsDestination.INSIGHTS -> InsightsScreen()
        }
    }
}
