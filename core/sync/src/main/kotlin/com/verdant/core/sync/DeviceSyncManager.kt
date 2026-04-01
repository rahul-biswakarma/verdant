package com.verdant.core.sync

import com.verdant.core.model.DeviceSignal
import com.verdant.core.model.repository.DeviceSignalRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages multi-device signal sync via Supabase Postgrest tables.
 *
 * Only lightweight signal summaries are synced — never raw data.
 * Example signal: { device: "mac", type: "focus_session", value: 45.0, unit: "minutes" }
 */
@Singleton
class DeviceSyncManager @Inject constructor(
    private val deviceSignalRepository: DeviceSignalRepository,
    private val supabase: SupabaseClient,
) {
    private fun userId(): String? = supabase.auth.currentUserOrNull()?.id

    /** Publishes a signal from this device to Supabase. */
    suspend fun publishSignal(
        deviceId: String,
        deviceRole: String,
        signalType: String,
        value: Double,
        unit: String,
    ) {
        val uid = userId() ?: return
        supabase.postgrest.from("sync_signals").insert(
            SyncSignalDto(
                userId = uid,
                deviceId = deviceId,
                deviceRole = deviceRole,
                signalType = signalType,
                value = value,
                unit = unit,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    /** Pulls all signals from other devices and stores them locally. */
    suspend fun pullSignals(thisDeviceId: String): Int {
        val uid = userId() ?: return 0
        val rows = supabase.postgrest.from("sync_signals")
            .select {
                filter {
                    eq("user_id", uid)
                    neq("device_id", thisDeviceId)
                }
            }
            .decodeList<SyncSignalDto>()

        var count = 0
        for (row in rows) {
            val signal = DeviceSignal(
                id = UUID.randomUUID().toString(),
                deviceId = row.deviceId,
                signalType = row.signalType,
                value = row.value,
                unit = row.unit,
                timestamp = row.timestamp,
                createdAt = System.currentTimeMillis(),
            )
            deviceSignalRepository.insert(signal)
            count++
        }

        // Consume processed signals
        if (rows.isNotEmpty()) {
            val ids = rows.mapNotNull { it.id }
            if (ids.isNotEmpty()) {
                supabase.postgrest.from("sync_signals").delete {
                    filter { isIn("id", ids) }
                }
            }
        }

        return count
    }

    /** Registers this device in the user's device list. */
    suspend fun registerDevice(deviceId: String, role: String, name: String) {
        val uid = userId() ?: return
        supabase.postgrest.from("user_devices").upsert(
            UserDeviceDto(
                userId = uid,
                deviceId = deviceId,
                role = role,
                name = name,
                lastSeen = System.currentTimeMillis(),
            )
        )
    }

    /** Gets all registered devices for the current user. */
    suspend fun getRegisteredDevices(): List<RegisteredDevice> {
        val uid = userId() ?: return emptyList()
        return supabase.postgrest.from("user_devices")
            .select { filter { eq("user_id", uid) } }
            .decodeList<UserDeviceDto>()
            .map { RegisteredDevice(it.deviceId, it.role, it.name, it.lastSeen) }
    }

    /** Removes a device registration. */
    suspend fun unregisterDevice(deviceId: String) {
        val uid = userId() ?: return
        supabase.postgrest.from("user_devices").delete {
            filter {
                eq("user_id", uid)
                eq("device_id", deviceId)
            }
        }
    }
}

data class RegisteredDevice(
    val id: String,
    val role: String,
    val name: String,
    val lastSeen: Long,
)

@Serializable
internal data class SyncSignalDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_role") val deviceRole: String,
    @SerialName("signal_type") val signalType: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
)

@Serializable
internal data class UserDeviceDto(
    @SerialName("user_id") val userId: String,
    @SerialName("device_id") val deviceId: String,
    val role: String,
    val name: String,
    @SerialName("last_seen") val lastSeen: Long,
)
