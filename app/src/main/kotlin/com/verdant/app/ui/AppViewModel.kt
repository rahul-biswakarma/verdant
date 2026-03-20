package com.verdant.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.designsystem.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppState(
    /** null = still loading from DataStore */
    val onboardingCompleted: Boolean? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: Long = 0xFF30A14EL,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    prefs: UserPreferencesDataStore,
) : ViewModel() {

    val state: StateFlow<AppState> = combine(
        prefs.onboardingCompleted,
        prefs.themeMode,
        prefs.accentColor,
    ) { completed, mode, color ->
        AppState(
            onboardingCompleted = completed,
            themeMode = ThemeMode.valueOf(mode),
            accentColor = color,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppState(), // null onboardingCompleted = loading
    )
}
