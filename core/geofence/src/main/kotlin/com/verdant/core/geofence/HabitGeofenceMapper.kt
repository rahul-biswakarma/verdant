package com.verdant.core.geofence

import javax.inject.Inject

class HabitGeofenceMapper @Inject constructor() {

    fun resolveHabitId(geofenceRequestId: String): String? {
        // Geofence request IDs follow format "verdant_geofence_{habitId}"
        val prefix = "verdant_geofence_"
        return if (geofenceRequestId.startsWith(prefix)) {
            geofenceRequestId.removePrefix(prefix)
        } else null
    }

    fun buildRequestId(habitId: String): String = "verdant_geofence_$habitId"
}
