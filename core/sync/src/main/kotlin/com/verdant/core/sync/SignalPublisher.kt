package com.verdant.core.sync

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Convenience class for publishing common signals from the phone.
 * Each method creates a lightweight signal (no raw data, just summaries).
 */
@Singleton
class SignalPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncManager: DeviceSyncManager,
) {
    @get:JvmName("_deviceId")
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private val deviceName: String by lazy {
        "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /** Registers this phone as the primary device. */
    suspend fun registerThisDevice() {
        syncManager.registerDevice(deviceId, ROLE_PHONE, deviceName)
    }

    suspend fun publishSteps(count: Int) {
        syncManager.publishSignal(deviceId, ROLE_PHONE, "steps", count.toDouble(), "steps")
    }

    suspend fun publishSleep(hours: Double) {
        syncManager.publishSignal(deviceId, ROLE_PHONE, "sleep", hours, "hours")
    }

    suspend fun publishScreenTime(minutes: Int) {
        syncManager.publishSignal(deviceId, ROLE_PHONE, "screen_time", minutes.toDouble(), "minutes")
    }

    suspend fun publishHabitCompletion(habitName: String) {
        // Anonymize habit name — only a short hash is published
        val anonymized = "habit_${habitName.sha256Short()}"
        syncManager.publishSignal(deviceId, ROLE_PHONE, "habit_completed", 1.0, anonymized)
    }

    suspend fun publishTransaction(amount: Double, category: String) {
        // Anonymize transaction category
        val anonymized = "cat_${category.sha256Short()}"
        syncManager.publishSignal(deviceId, ROLE_PHONE, "transaction", amount, anonymized)
    }

    suspend fun publishActiveMinutes(minutes: Int) {
        syncManager.publishSignal(deviceId, ROLE_PHONE, "active_minutes", minutes.toDouble(), "minutes")
    }

    fun getDeviceId(): String = deviceId

    companion object {
        const val ROLE_PHONE = "phone"
        const val ROLE_MAC = "mac"
        const val ROLE_WATCH = "watch"
        const val ROLE_TABLET = "tablet"
    }
}

/** Returns the first 8 hex characters of the SHA-256 hash of this string. */
private fun String.sha256Short(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(lowercase().toByteArray())
    return digest.take(4).joinToString("") { "%02x".format(it) }
}
