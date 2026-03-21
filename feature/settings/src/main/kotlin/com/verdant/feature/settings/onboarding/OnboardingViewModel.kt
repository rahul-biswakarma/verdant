package com.verdant.feature.settings.onboarding

import android.content.Context
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

    fun completeOnboarding(onDone: () -> Unit) = viewModelScope.launch {
        prefs.setOnboardingCompleted(true)
        onDone()
    }

    fun signInWithGoogle(activityContext: Context, webClientId: String, onDone: () -> Unit) =
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activityContext, webClientId)
            if (result.isSuccess) {
                prefs.setOnboardingCompleted(true)
                onDone()
            }
        }
}
