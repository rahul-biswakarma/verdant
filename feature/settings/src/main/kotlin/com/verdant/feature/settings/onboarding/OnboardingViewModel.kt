package com.verdant.feature.settings.onboarding

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.common.auth.AuthRepository
import com.verdant.core.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _emailSignInError = MutableStateFlow<String?>(null)
    val emailSignInError: StateFlow<String?> = _emailSignInError.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

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

    fun signInWithEmail(email: String, password: String) =
        viewModelScope.launch {
            _isSigningIn.value = true
            _emailSignInError.value = null
            val result = authRepository.signInWithEmail(email, password)
            _isSigningIn.value = false
            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message ?: "Sign-in failed"
                Log.e("OnboardingViewModel", "Email sign-in failed", result.exceptionOrNull())
                _emailSignInError.value = msg
            }
        }

    fun clearEmailError() {
        _emailSignInError.value = null
    }
}
