package com.verdant.core.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.verdant.core.model.DeviceSignal
import com.verdant.core.model.repository.DeviceSignalRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages multi-device signal sync via Firebase Realtime Database.
 *
 * Only lightweight signal summaries are synced -- never raw data.
 * Example signal: { device: "mac", type: "focus_session", value: 45.0, unit: "minutes" }
 */
@Singleton
class DeviceSyncManager @Inject constructor(
    private val deviceSignalRepository: DeviceSignalRepository,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    private fun signalsRef() = userId?.let {
        database.reference.child("users").child(it).child("signals")
    }

    private fun devicesRef() = userId?.let {
        database.reference.child("users").child(it).child("devices")
    }

    /** Publishes a signal from this device to Firebase RTDB. */
    suspend fun publishSignal(
        deviceId: String,
        deviceRole: String,
        signalType: String,
        value: Double,
        unit: String,
    ) {
        val ref = signalsRef() ?: return
        val signal = mapOf(
            "deviceId" to deviceId,
            "deviceRole" to deviceRole,
            "signalType" to signalType,
            "value" to value,
            "unit" to unit,
            "timestamp" to System.currentTimeMillis(),
        )
        ref.push().setValue(signal).await()
    }

    /** Pulls all signals from other devices and stores them locally. */
    suspend fun pullSignals(thisDeviceId: String): Int {
        val ref = signalsRef() ?: return 0
        val snapshot = ref.get().await()
        var count = 0

        for (child in snapshot.children) {
            val deviceId = child.child("deviceId").getValue(String::class.java) ?: continue
            if (deviceId == thisDeviceId) continue // Skip own signals

            val signal = DeviceSignal(
                id = UUID.randomUUID().toString(),
                deviceId = deviceId,
                signalType = child.child("signalType").getValue(String::class.java) ?: continue,
                value = child.child("value").getValue(Double::class.java) ?: continue,
                unit = child.child("unit").getValue(String::class.java) ?: "",
                timestamp = child.child("timestamp").getValue(Long::class.java) ?: continue,
                createdAt = System.currentTimeMillis(),
            )
            deviceSignalRepository.insert(signal)
            child.ref.removeValue().await() // Consume the signal
            count++
        }
        return count
    }

    /** Registers this device in the user's device list. */
    suspend fun registerDevice(deviceId: String, role: String, name: String) {
        val ref = devicesRef() ?: return
        val device = mapOf(
            "role" to role,
            "name" to name,
            "lastSeen" to System.currentTimeMillis(),
        )
        ref.child(deviceId).setValue(device).await()
    }

    /** Gets all registered devices for the current user. */
    suspend fun getRegisteredDevices(): List<RegisteredDevice> {
        val ref = devicesRef() ?: return emptyList()
        val snapshot = ref.get().await()
        return snapshot.children.mapNotNull { child ->
            RegisteredDevice(
                id = child.key ?: return@mapNotNull null,
                role = child.child("role").getValue(String::class.java) ?: "unknown",
                name = child.child("name").getValue(String::class.java) ?: "Unknown Device",
                lastSeen = child.child("lastSeen").getValue(Long::class.java) ?: 0L,
            )
        }
    }

    /** Removes a device registration. */
    suspend fun unregisterDevice(deviceId: String) {
        devicesRef()?.child(deviceId)?.removeValue()?.await()
    }
}

data class RegisteredDevice(
    val id: String,
    val role: String,
    val name: String,
    val lastSeen: Long,
)
