package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.model.DeviceStat
import com.verdant.core.model.DeviceStatType
import com.verdant.core.model.repository.DeviceStatRepository
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.devicestats.BatteryTracker
import com.verdant.core.devicestats.CalendarReader
import com.verdant.core.devicestats.UsageStatsReader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.UUID

@HiltWorker
class DeviceStatsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageStatsReader: UsageStatsReader,
    private val calendarReader: CalendarReader,
    private val batteryTracker: BatteryTracker,
    private val deviceStatRepository: DeviceStatRepository,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_device_stats_sync"
    }

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val lastSync = prefs.lastDeviceStatsSyncTime.first()
        val stats = mutableListOf<DeviceStat>()

        if (prefs.screenTimeTrackingEnabled.first() && usageStatsReader.hasPermission()) {
            val screenTime = usageStatsReader.getScreenTimeMinutes(lastSync, now)
            stats.add(DeviceStat(
                id = UUID.randomUUID().toString(),
                statType = DeviceStatType.SCREEN_TIME,
                value = screenTime,
                detail = null,
                recordedDate = now,
                createdAt = now,
            ))
        }

        if (prefs.calendarSyncEnabled.first() && calendarReader.hasPermission()) {
            val busyHours = calendarReader.getBusyHours(now)
            stats.add(DeviceStat(
                id = UUID.randomUUID().toString(),
                statType = DeviceStatType.CALENDAR_BUSY_HOURS,
                value = busyHours,
                detail = null,
                recordedDate = now,
                createdAt = now,
            ))
        }

        val batteryDrain = batteryTracker.getBatteryDrainPercentSince(lastSync)
        if (batteryDrain > 0) {
            stats.add(DeviceStat(
                id = UUID.randomUUID().toString(),
                statType = DeviceStatType.BATTERY_DRAIN,
                value = batteryDrain,
                detail = null,
                recordedDate = now,
                createdAt = now,
            ))
        }

        if (stats.isNotEmpty()) {
            deviceStatRepository.insertAll(stats)
        }

        prefs.setLastDeviceStatsSyncTime(now)
        return Result.success()
    }
}
