package com.verdant.core.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GeofenceTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // TODO: Handle ENTER/EXIT transitions, enqueue GeofenceTransitionWorker
    }
}
