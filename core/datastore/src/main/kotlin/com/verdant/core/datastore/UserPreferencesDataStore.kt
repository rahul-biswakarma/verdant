package com.verdant.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "verdant_prefs")

/**
 * Central DataStore for all user preferences.
 *
 * Appearance:
 *  - [themeMode]               — "LIGHT" | "DARK" | "SYSTEM" (default "SYSTEM")
 *  - [accentColor]             — ARGB Long (default Warm Charcoal 0xFF2E2D2B)
 *  - [firstDayOfWeek]          — "MONDAY" | "SUNDAY" (default "MONDAY")
 *  - [useDynamicColor]         — Material You dynamic color (default true)
 *
 * Notifications:
 *  - [notificationsEnabled]    — master kill-switch (default true)
 *  - [quietHoursStart]         — 24h hour when quiet period begins (default 22 = 10 PM)
 *  - [quietHoursEnd]           — 24h hour when quiet period ends   (default 7  =  7 AM)
 *  - [maxNudgesPerDay]         — cap on streak-nudge notifications  (default 5)
 *  - [nudgeTone]               — "gentle" | "motivating" | "direct" (default "motivating")
 *  - [dailyMotivationEnabled]  — morning motivation card             (default true)
 *  - [weeklySummaryEnabled]    — weekly summary notification         (default true)
 *  - [weeklySummaryDay]        — DayOfWeek.value 1-7 (default 7 = Sunday)
 *  - [weeklySummaryHour]       — 0-23 (default 19 = 7 PM)
 *  - [streakAlertsEnabled]     — nudges for at-risk streaks          (default true)
 *
 * Privacy:
 *  - [llmDataSharing]          — share anonymized stats with AI (default false)
 *
 * Onboarding:
 *  - [onboardingCompleted]     — has user completed onboarding (default false)
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        // Appearance
        val USE_DYNAMIC_COLOR    = booleanPreferencesKey("use_dynamic_color")
        val THEME_MODE           = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR         = longPreferencesKey("accent_color")
        val FIRST_DAY_OF_WEEK    = stringPreferencesKey("first_day_of_week")

        // Notification master toggle
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

        // Quiet hours (24-hour clock values)
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END   = intPreferencesKey("quiet_hours_end")

        // Nudge limits
        val MAX_NUDGES_PER_DAY = intPreferencesKey("max_nudges_per_day")

        // Nudge tone
        val NUDGE_TONE = stringPreferencesKey("nudge_tone")

        // Feature toggles
        val DAILY_MOTIVATION_ENABLED = booleanPreferencesKey("daily_motivation_enabled")
        val WEEKLY_SUMMARY_ENABLED   = booleanPreferencesKey("weekly_summary_enabled")
        val WEEKLY_SUMMARY_DAY       = intPreferencesKey("weekly_summary_day")
        val WEEKLY_SUMMARY_HOUR      = intPreferencesKey("weekly_summary_hour")
        val STREAK_ALERTS_ENABLED    = booleanPreferencesKey("streak_alerts_enabled")

        // Privacy
        val LLM_DATA_SHARING = booleanPreferencesKey("llm_data_sharing")

        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // On-device AI model
        val ON_DEVICE_MODEL_DOWNLOADED = booleanPreferencesKey("on_device_model_downloaded")
        val ON_DEVICE_MODEL_PATH = stringPreferencesKey("on_device_model_path")

        // Finance product
        val SMS_PERMISSION_GRANTED = booleanPreferencesKey("sms_permission_granted")
        val FINANCE_ONBOARDING_COMPLETED = booleanPreferencesKey("finance_onboarding_completed")
        val LAST_SMS_PROCESSED_TIME = longPreferencesKey("last_sms_processed_time")
        val FINANCE_ALERTS_ENABLED = booleanPreferencesKey("finance_alerts_enabled")
        val MONTHLY_REPORT_ENABLED = booleanPreferencesKey("monthly_report_enabled")
        val FINANCE_DATA_SHARING = booleanPreferencesKey("finance_data_sharing")

        // Data sources
        val HEALTH_CONNECT_ENABLED = booleanPreferencesKey("health_connect_enabled")
        val ACTIVITY_RECOGNITION_ENABLED = booleanPreferencesKey("activity_recognition_enabled")
        val SCREEN_TIME_TRACKING_ENABLED = booleanPreferencesKey("screen_time_tracking_enabled")
        val CALENDAR_SYNC_ENABLED = booleanPreferencesKey("calendar_sync_enabled")
        val WEATHER_TRACKING_ENABLED = booleanPreferencesKey("weather_tracking_enabled")
        val NOTIFICATION_TRACKING_ENABLED = booleanPreferencesKey("notification_tracking_enabled")
        val LIFE_DASHBOARD_ONBOARDING_COMPLETED = booleanPreferencesKey("life_dashboard_onboarding_completed")
        val LAST_HEALTH_SYNC_TIME = longPreferencesKey("last_health_sync_time")
        val LAST_DEVICE_STATS_SYNC_TIME = longPreferencesKey("last_device_stats_sync_time")

        // Player system
        val EVOLUTION_PATH = stringPreferencesKey("evolution_path")
        val PLAYER_LEVEL_SHOWN = intPreferencesKey("player_level_shown")
    }
    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.USE_DYNAMIC_COLOR] ?: true
    }

    suspend fun setUseDynamicColor(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = value }
    }

    /** One of "LIGHT", "DARK", "SYSTEM". Default: "SYSTEM". */
    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "SYSTEM"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    /** ARGB color as Long. Default: Warm Charcoal 0xFF2E2D2B. */
    val accentColor: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCENT_COLOR] ?: 0xFF2E2D2BL
    }

    suspend fun setAccentColor(color: Long) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = color }
    }

    /** One of "MONDAY", "SUNDAY". Default: "MONDAY". */
    val firstDayOfWeek: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.FIRST_DAY_OF_WEEK] ?: "MONDAY"
    }

    suspend fun setFirstDayOfWeek(value: String) {
        context.dataStore.edit { it[Keys.FIRST_DAY_OF_WEEK] = value }
    }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = value }
    }
    /** Hour (0–23) at which the quiet period begins. Default: 22 (10 PM). */
    val quietHoursStart: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.QUIET_HOURS_START] ?: 22
    }

    /** Hour (0–23) at which the quiet period ends. Default: 7 (7 AM). */
    val quietHoursEnd: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.QUIET_HOURS_END] ?: 7
    }

    suspend fun setQuietHours(startHour: Int, endHour: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.QUIET_HOURS_START] = startHour
            prefs[Keys.QUIET_HOURS_END]   = endHour
        }
    }
    /** Maximum streak-nudge notifications per day. Default: 5. */
    val maxNudgesPerDay: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MAX_NUDGES_PER_DAY] ?: 5
    }

    suspend fun setMaxNudgesPerDay(value: Int) {
        context.dataStore.edit { it[Keys.MAX_NUDGES_PER_DAY] = value.coerceIn(1, 10) }
    }
    /** One of "gentle", "motivating", "direct". Default: "motivating". */
    val nudgeTone: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.NUDGE_TONE] ?: NudgeTone.MOTIVATING.key
    }

    suspend fun setNudgeTone(tone: NudgeTone) {
        context.dataStore.edit { it[Keys.NUDGE_TONE] = tone.key }
    }
    val dailyMotivationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DAILY_MOTIVATION_ENABLED] ?: true
    }

    suspend fun setDailyMotivationEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.DAILY_MOTIVATION_ENABLED] = value }
    }

    val weeklySummaryEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.WEEKLY_SUMMARY_ENABLED] ?: true
    }

    suspend fun setWeeklySummaryEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.WEEKLY_SUMMARY_ENABLED] = value }
    }

    /** DayOfWeek.value: 1 = Monday … 7 = Sunday. Default: 7 (Sunday). */
    val weeklySummaryDay: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.WEEKLY_SUMMARY_DAY] ?: 7
    }

    suspend fun setWeeklySummaryDay(dayValue: Int) {
        context.dataStore.edit { it[Keys.WEEKLY_SUMMARY_DAY] = dayValue.coerceIn(1, 7) }
    }

    /** Hour (0–23) for the weekly summary notification. Default: 19 (7 PM). */
    val weeklySummaryHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.WEEKLY_SUMMARY_HOUR] ?: 19
    }

    suspend fun setWeeklySummaryHour(hour: Int) {
        context.dataStore.edit { it[Keys.WEEKLY_SUMMARY_HOUR] = hour.coerceIn(0, 23) }
    }

    val streakAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.STREAK_ALERTS_ENABLED] ?: true
    }

    suspend fun setStreakAlertsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.STREAK_ALERTS_ENABLED] = value }
    }
    val llmDataSharing: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LLM_DATA_SHARING] ?: false
    }

    suspend fun setLlmDataSharing(value: Boolean) {
        context.dataStore.edit { it[Keys.LLM_DATA_SHARING] = value }
    }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = value }
    }

    val onDeviceModelDownloaded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ON_DEVICE_MODEL_DOWNLOADED] ?: false
    }

    suspend fun setOnDeviceModelDownloaded(value: Boolean) {
        context.dataStore.edit { it[Keys.ON_DEVICE_MODEL_DOWNLOADED] = value }
    }

    // ── Finance product ────────────────────────────────────────

    val smsPermissionGranted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SMS_PERMISSION_GRANTED] ?: false
    }

    suspend fun setSmsPermissionGranted(value: Boolean) {
        context.dataStore.edit { it[Keys.SMS_PERMISSION_GRANTED] = value }
    }

    val financeOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.FINANCE_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setFinanceOnboardingCompleted(value: Boolean) {
        context.dataStore.edit { it[Keys.FINANCE_ONBOARDING_COMPLETED] = value }
    }

    val lastSmsProcessedTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SMS_PROCESSED_TIME] ?: 0L
    }

    suspend fun setLastSmsProcessedTime(value: Long) {
        context.dataStore.edit { it[Keys.LAST_SMS_PROCESSED_TIME] = value }
    }

    val financeAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.FINANCE_ALERTS_ENABLED] ?: true
    }

    suspend fun setFinanceAlertsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.FINANCE_ALERTS_ENABLED] = value }
    }

    val monthlyReportEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MONTHLY_REPORT_ENABLED] ?: true
    }

    suspend fun setMonthlyReportEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.MONTHLY_REPORT_ENABLED] = value }
    }

    val financeDataSharing: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.FINANCE_DATA_SHARING] ?: false
    }

    suspend fun setFinanceDataSharing(value: Boolean) {
        context.dataStore.edit { it[Keys.FINANCE_DATA_SHARING] = value }
    }

    // ── Data Sources ───────────────────────────────────────────

    val healthConnectEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HEALTH_CONNECT_ENABLED] ?: false
    }

    suspend fun setHealthConnectEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.HEALTH_CONNECT_ENABLED] = value }
    }

    val activityRecognitionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVITY_RECOGNITION_ENABLED] ?: false
    }

    suspend fun setActivityRecognitionEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.ACTIVITY_RECOGNITION_ENABLED] = value }
    }

    val screenTimeTrackingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCREEN_TIME_TRACKING_ENABLED] ?: false
    }

    suspend fun setScreenTimeTrackingEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.SCREEN_TIME_TRACKING_ENABLED] = value }
    }

    val calendarSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CALENDAR_SYNC_ENABLED] ?: false
    }

    suspend fun setCalendarSyncEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.CALENDAR_SYNC_ENABLED] = value }
    }

    val weatherTrackingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.WEATHER_TRACKING_ENABLED] ?: false
    }

    suspend fun setWeatherTrackingEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.WEATHER_TRACKING_ENABLED] = value }
    }

    val notificationTrackingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_TRACKING_ENABLED] ?: false
    }

    suspend fun setNotificationTrackingEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_TRACKING_ENABLED] = value }
    }

    val lifeDashboardOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LIFE_DASHBOARD_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setLifeDashboardOnboardingCompleted(value: Boolean) {
        context.dataStore.edit { it[Keys.LIFE_DASHBOARD_ONBOARDING_COMPLETED] = value }
    }

    val lastHealthSyncTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_HEALTH_SYNC_TIME] ?: 0L
    }

    suspend fun setLastHealthSyncTime(value: Long) {
        context.dataStore.edit { it[Keys.LAST_HEALTH_SYNC_TIME] = value }
    }

    val lastDeviceStatsSyncTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_DEVICE_STATS_SYNC_TIME] ?: 0L
    }

    suspend fun setLastDeviceStatsSyncTime(value: Long) {
        context.dataStore.edit { it[Keys.LAST_DEVICE_STATS_SYNC_TIME] = value }
    }

    // ── Player System ───────────────────────────────────────────

    val evolutionPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.EVOLUTION_PATH] ?: "BALANCE"
    }

    suspend fun setEvolutionPath(value: String) {
        context.dataStore.edit { it[Keys.EVOLUTION_PATH] = value }
    }

    val playerLevelShown: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.PLAYER_LEVEL_SHOWN] ?: 0
    }

    suspend fun setPlayerLevelShown(value: Int) {
        context.dataStore.edit { it[Keys.PLAYER_LEVEL_SHOWN] = value }
    }

    /** Resets all preferences to defaults (used by "Delete all data"). */
    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}

enum class NudgeTone(val key: String, val label: String) {
    GENTLE("gentle", "Encouraging"),
    MOTIVATING("motivating", "Neutral"),
    DIRECT("direct", "Tough Love");

    companion object {
        fun fromKey(key: String): NudgeTone =
            entries.firstOrNull { it.key == key } ?: MOTIVATING
    }
}
