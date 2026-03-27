package com.verdant.core.devicestats

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CalendarReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun getBusyHours(date: Long): Double {
        // TODO: Read from CalendarContract
        return 0.0
    }

    fun hasPermission(): Boolean {
        // TODO: Check READ_CALENDAR permission
        return false
    }
}
