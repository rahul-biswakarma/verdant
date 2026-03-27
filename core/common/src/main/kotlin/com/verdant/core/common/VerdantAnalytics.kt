package com.verdant.core.common

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight analytics event logger. Currently logs to Logcat with TAG "VerdantAnalytics".
 * Can be extended to Firebase Analytics or other providers.
 *
 * No PII is ever logged — only event names and anonymous counts.
 */
@Singleton
class VerdantAnalytics @Inject constructor() {

    fun trackEvent(event: String, params: Map<String, Any> = emptyMap()) {
        Log.d(TAG, "Event: $event ${if (params.isNotEmpty()) params else ""}")
    }

    // Predefined events
    fun habitCreated(trackingType: String) = trackEvent("habit_created", mapOf("type" to trackingType))
    fun habitCompleted(trackingType: String) = trackEvent("habit_completed", mapOf("type" to trackingType))
    fun habitSkipped() = trackEvent("habit_skipped")
    fun streakMilestone(days: Int) = trackEvent("streak_milestone", mapOf("days" to days))
    fun aiInsightGenerated(type: String) = trackEvent("ai_insight_generated", mapOf("type" to type))
    fun aiInsightDismissed(type: String) = trackEvent("ai_insight_dismissed", mapOf("type" to type))
    fun voiceLogSuccess() = trackEvent("voice_log_success")
    fun brainDumpUsed(matchCount: Int) = trackEvent("brain_dump_used", mapOf("matches" to matchCount))
    fun exportCompleted(format: String) = trackEvent("export_completed", mapOf("format" to format))
    fun questCompleted(difficulty: String) = trackEvent("quest_completed", mapOf("difficulty" to difficulty))
    fun levelUp(newLevel: Int) = trackEvent("level_up", mapOf("level" to newLevel))
    fun deviceSynced(deviceRole: String) = trackEvent("device_synced", mapOf("role" to deviceRole))
    fun budgetAlertTriggered() = trackEvent("budget_alert")
    fun widgetInteraction(widgetType: String) = trackEvent("widget_interaction", mapOf("type" to widgetType))

    companion object {
        private const val TAG = "VerdantAnalytics"
    }
}
