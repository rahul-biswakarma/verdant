package com.verdant.feature.settings.onboarding

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.common.auth.AuthRepository
import com.verdant.core.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = authRepository.isSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun markOnboardingDone() = viewModelScope.launch {
        prefs.setOnboardingCompleted(true)
    }

    fun signInWithGoogle(activityContext: Context, webClientId: String) =
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activityContext, webClientId)
            if (result.isFailure) {
                Log.e("OnboardingViewModel", "Google sign-in failed", result.exceptionOrNull())
            }
            // Navigation is driven reactively by isSignedIn in OnboardingScreen
        }
}
