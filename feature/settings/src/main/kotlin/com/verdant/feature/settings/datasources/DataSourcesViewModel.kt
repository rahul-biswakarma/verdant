package com.verdant.feature.settings.datasources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataSourcesUiState(
    val healthConnectEnabled: Boolean = false,
    val activityRecognitionEnabled: Boolean = false,
    val screenTimeTrackingEnabled: Boolean = false,
    val calendarSyncEnabled: Boolean = false,
    val notificationTrackingEnabled: Boolean = false,
    val weatherTrackingEnabled: Boolean = false,
)

@HiltViewModel
class DataSourcesViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    val uiState = combine(
        prefs.healthConnectEnabled,
        prefs.activityRecognitionEnabled,
        prefs.screenTimeTrackingEnabled,
        prefs.calendarSyncEnabled,
        prefs.notificationTrackingEnabled,
        prefs.weatherTrackingEnabled,
    ) { values ->
        DataSourcesUiState(
            healthConnectEnabled = values[0] as Boolean,
            activityRecognitionEnabled = values[1] as Boolean,
            screenTimeTrackingEnabled = values[2] as Boolean,
            calendarSyncEnabled = values[3] as Boolean,
            notificationTrackingEnabled = values[4] as Boolean,
            weatherTrackingEnabled = values[5] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataSourcesUiState())

    fun toggleHealthConnect(enabled: Boolean) {
        viewModelScope.launch { prefs.setHealthConnectEnabled(enabled) }
    }

    fun toggleActivityRecognition(enabled: Boolean) {
        viewModelScope.launch { prefs.setActivityRecognitionEnabled(enabled) }
    }

    fun toggleScreenTimeTracking(enabled: Boolean) {
        viewModelScope.launch { prefs.setScreenTimeTrackingEnabled(enabled) }
    }

    fun toggleCalendarSync(enabled: Boolean) {
        viewModelScope.launch { prefs.setCalendarSyncEnabled(enabled) }
    }

    fun toggleNotificationTracking(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationTrackingEnabled(enabled) }
    }

    fun toggleWeatherTracking(enabled: Boolean) {
        viewModelScope.launch { prefs.setWeatherTrackingEnabled(enabled) }
    }
}
