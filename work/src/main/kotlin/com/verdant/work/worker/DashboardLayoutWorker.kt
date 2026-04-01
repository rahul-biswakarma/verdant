package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.genui.generation.DashboardGenerator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Daily worker that generates a personalised dashboard layout via LLM.
 * Scheduled at 5:30 AM so the fresh layout is ready when the user opens the
 * app in the morning.
 *
 * Falls back gracefully: if generation fails, the app uses the cached layout
 * or the static fallback.
 */
@HiltWorker
class DashboardLayoutWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dashboardGenerator: DashboardGenerator,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_dashboard_layout"
    }

    override suspend fun doWork(): Result {
        // Skip if Gen UI is disabled
        val enabled = prefs.genUiEnabled.first()
        if (!enabled) return Result.success()

        // Skip if user hasn't opted into data sharing
        val dataSharingEnabled = prefs.llmDataSharing.first()
        if (!dataSharingEnabled) return Result.success()

        return try {
            dashboardGenerator.generate()
            Result.success()
        } catch (_: Exception) {
            // Non-critical — the app will use cached or fallback layout
            Result.success()
        }
    }
}
