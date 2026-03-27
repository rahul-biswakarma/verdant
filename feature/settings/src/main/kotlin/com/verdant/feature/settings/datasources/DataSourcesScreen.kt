package com.verdant.feature.settings.datasources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSourcesScreen(
    onBack: () -> Unit,
    viewModel: DataSourcesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Sources") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TablerIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Connect data sources to unlock deeper insights about your life patterns.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                SectionHeader("Health & Body")
            }

            item {
                DataSourceCard(
                    title = "Health Connect",
                    description = "Steps, sleep, heart rate, exercise, weight, and hydration from health apps",
                    enabled = uiState.healthConnectEnabled,
                    onToggle = viewModel::toggleHealthConnect,
                )
            }

            item {
                DataSourceCard(
                    title = "Activity Recognition",
                    description = "Automatically detect walking, running, cycling, and stationary periods",
                    enabled = uiState.activityRecognitionEnabled,
                    onToggle = viewModel::toggleActivityRecognition,
                )
            }

            item {
                SectionHeader("Device & Productivity")
            }

            item {
                DataSourceCard(
                    title = "Screen Time",
                    description = "Track app usage and screen-on time for digital wellness insights",
                    enabled = uiState.screenTimeTrackingEnabled,
                    onToggle = viewModel::toggleScreenTimeTracking,
                )
            }

            item {
                DataSourceCard(
                    title = "Calendar",
                    description = "Sync busy hours from your calendar to understand scheduling patterns",
                    enabled = uiState.calendarSyncEnabled,
                    onToggle = viewModel::toggleCalendarSync,
                )
            }

            item {
                DataSourceCard(
                    title = "Notification Tracking",
                    description = "Count daily notifications as a stress and distraction signal",
                    enabled = uiState.notificationTrackingEnabled,
                    onToggle = viewModel::toggleNotificationTracking,
                )
            }

            item {
                SectionHeader("Environment")
            }

            item {
                DataSourceCard(
                    title = "Weather",
                    description = "Track weather conditions to find correlations with your habits",
                    enabled = uiState.weatherTrackingEnabled,
                    onToggle = viewModel::toggleWeatherTracking,
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun DataSourceCard(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
