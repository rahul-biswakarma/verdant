package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.database.dao.WeatherDao
import com.verdant.core.database.entity.WeatherSnapshotEntity
import com.verdant.core.datastore.UserPreferencesDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.UUID

@HiltWorker
class WeatherSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val weatherDao: WeatherDao,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_weather_sync"
    }

    override suspend fun doWork(): Result {
        if (!prefs.weatherTrackingEnabled.first()) return Result.success()

        // TODO: Call Open-Meteo API with user's location
        // For now, stub worker that will be connected when WeatherContextProvider is implemented
        return Result.success()
    }
}
