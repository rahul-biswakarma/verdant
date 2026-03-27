package com.verdant.core.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class VoiceRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed class VoiceState {
        data object Idle : VoiceState()
        data object Listening : VoiceState()
        data class Result(val text: String) : VoiceState()
        data class Error(val message: String) : VoiceState()
    }

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: Flow<VoiceState> = _state

    fun startListening() {
        // TODO: Wrap SpeechRecognizer
        _state.value = VoiceState.Listening
    }

    fun stopListening() {
        _state.value = VoiceState.Idle
    }
}
