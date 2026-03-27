package com.verdant.core.context

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContextSignalProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class DeviceContext(
        val isCharging: Boolean,
        val isHeadphonesConnected: Boolean,
        val isWeekend: Boolean,
        val hourOfDay: Int,
        val isConnectedToWifi: Boolean,
    )

    fun readDeviceContext(): DeviceContext {
        // TODO: Read actual device state
        val calendar = java.util.Calendar.getInstance()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        return DeviceContext(
            isCharging = false,
            isHeadphonesConnected = false,
            isWeekend = dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY,
            hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY),
            isConnectedToWifi = false,
        )
    }
}
