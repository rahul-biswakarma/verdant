package com.verdant.core.voice

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class VoiceChatManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun speakText(text: String) {
        // TODO: TextToSpeech output
    }

    fun stopSpeaking() {
        // TODO: Stop TTS
    }
}
