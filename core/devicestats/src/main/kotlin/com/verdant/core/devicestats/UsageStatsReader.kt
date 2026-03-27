package com.verdant.core.devicestats

import android.content.Context
import com.verdant.core.model.DeviceStat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UsageStatsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun getScreenTimeMinutes(startTime: Long, endTime: Long): Double {
        // TODO: Read from UsageStatsManager
        return 0.0
    }

    suspend fun getAppUsageStats(startTime: Long, endTime: Long): List<DeviceStat> {
        // TODO: Read per-app usage from UsageStatsManager
        return emptyList()
    }

    fun hasPermission(): Boolean {
        // TODO: Check PACKAGE_USAGE_STATS permission
        return false
    }
}
