package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.database.dao.QuestDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class QuestGeneratorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val questDao: QuestDao,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_quest_generator"
    }

    override suspend fun doWork(): Result {
        // TODO: Generate personalized quests from emotional engine + pattern data
        // Use AI to create quests targeting weak spots
        questDao.deleteExpired()
        return Result.success()
    }
}
