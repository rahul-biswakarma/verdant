package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.verdant.core.sync.DeviceSyncManager
import com.verdant.core.sync.SignalPublisher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodically pulls signals from other devices via Firebase RTDB
 * and pushes this device's heartbeat. Runs every 1 hour with network constraint.
 */
@HiltWorker
class CrossDeviceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: DeviceSyncManager,
    private val signalPublisher: SignalPublisher,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val deviceId = signalPublisher.getDeviceId()

            // Register/heartbeat this device
            signalPublisher.registerThisDevice()

            // Pull signals from other devices
            syncManager.pullSignals(deviceId)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "verdant_cross_device_sync"

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
