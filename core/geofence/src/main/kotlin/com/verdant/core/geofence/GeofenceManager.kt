package com.verdant.core.geofence

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun registerGeofence(habitId: String, lat: Double, lon: Double, radiusMeters: Float) {
        // TODO: Wrap Geofencing API, register fence per habitId
    }

    fun removeGeofence(habitId: String) {
        // TODO: Remove registered geofence
    }

    fun removeAllGeofences() {
        // TODO: Remove all geofences
    }
}
