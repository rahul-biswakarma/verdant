package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.datastore.UserPreferencesDataStore
import com.verdant.core.health.HealthConnectClient
import com.verdant.core.model.HealthRecord
import com.verdant.core.model.HealthRecordType
import com.verdant.core.model.repository.HealthRecordRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val healthClient: HealthConnectClient,
    private val healthRecordRepository: HealthRecordRepository,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!prefs.healthConnectEnabled.first()) return Result.success()
        if (!healthClient.isAvailable()) return Result.success()

        return runCatching {
            val lastSync = prefs.lastHealthSyncTime.first()
            val now = System.currentTimeMillis()

            HealthRecordType.entries.forEach { type ->
                val records = healthClient.readRecords(type, lastSync, now)
                if (records.isNotEmpty()) {
                    healthRecordRepository.insertAll(records)
                }
            }

            prefs.setLastHealthSyncTime(now)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "verdant_health_sync"
    }

}
