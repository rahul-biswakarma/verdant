package com.verdant.core.devicestats

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BatteryTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getBatteryLevel(): Int {
        // TODO: Read from BatteryManager
        return -1
    }

    fun getBatteryDrainPercentSince(startTime: Long): Double {
        // TODO: Calculate drain since timestamp
        return 0.0
    }
}
