package com.verdant.work.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.verdant.core.model.repository.PlayerProfileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class XPComputeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val playerProfileRepository: PlayerProfileRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "verdant_xp_compute"
    }

    override suspend fun doWork(): Result {
        // TODO: Calculate XP from today's completions
        // 10 XP per habit completion, 50 for all daily, streak multipliers
        // Update player_profile with new XP and level
        return Result.success()
    }
}
