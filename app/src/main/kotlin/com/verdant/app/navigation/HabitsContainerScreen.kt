package com.verdant.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.verdant.feature.analytics.AnalyticsScreen
import com.verdant.feature.habits.HabitsScreen
import com.verdant.feature.habits.create.CreateHabitScreen

@Composable
fun HabitsContainerScreen(
    selectedTab: HabitsTab,
    onCreateHabit: () -> Unit,
    onHabitDetail: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    onBackFromCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (selectedTab) {
        HabitsTab.LIST -> HabitsScreen(
            onCreateHabit = onCreateHabit,
            onHabitDetail = onHabitDetail,
            onEditHabit = onEditHabit,
            modifier = modifier.fillMaxSize(),
        )
        HabitsTab.ANALYTICS -> AnalyticsScreen(modifier = modifier.fillMaxSize())
        HabitsTab.CREATE -> CreateHabitScreen(
            onNavigateBack = onBackFromCreate,
            modifier = modifier.fillMaxSize(),
        )
    }
}
