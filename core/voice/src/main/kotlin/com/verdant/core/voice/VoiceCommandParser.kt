package com.verdant.core.voice

import javax.inject.Inject

class VoiceCommandParser @Inject constructor() {

    data class HabitLogCommand(
        val habitName: String,
        val value: Double?,
        val unit: String?,
    )

    fun parse(text: String): List<HabitLogCommand> {
        // TODO: Parse recognized text into habit log commands
        // e.g. "Logged 5km run and 30 minutes reading"
        return emptyList()
    }
}
