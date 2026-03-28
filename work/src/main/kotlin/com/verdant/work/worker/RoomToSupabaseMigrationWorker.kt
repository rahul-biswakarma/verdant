package com.verdant.work.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.verdant.core.database.VerdantDatabase
import com.verdant.core.database.entity.toDomain
import com.verdant.core.supabase.dto.toDto
import com.verdant.core.supabase.dto.toDomain as habitDtoToDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-time worker that migrates existing Room data to Supabase
 * for users upgrading from the local-only version.
 */
@HiltWorker
class RoomToSupabaseMigrationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: VerdantDatabase,
    private val supabase: SupabaseClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.retry()

        return try {
            setProgress(workDataOf("status" to "Migrating habits..."))
            migrateHabits(userId)

            setProgress(workDataOf("status" to "Migrating habit entries..."))
            migrateHabitEntries(userId)

            setProgress(workDataOf("status" to "Migrating labels..."))
            migrateLabels(userId)

            setProgress(workDataOf("status" to "Migrating transactions..."))
            migrateTransactions(userId)

            setProgress(workDataOf("status" to "Migrating emotional context..."))
            migrateEmotionalContext(userId)

            setProgress(workDataOf("status" to "Migration complete"))
            Log.i(TAG, "Room to Supabase migration complete")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun migrateHabits(userId: String) {
        val habits = database.habitDao().getAll()
        if (habits.isEmpty()) return
        val dtos = habits.map { it.toDomain().toDto(userId) }
        supabase.postgrest["habits"].upsert(dtos)
    }

    private suspend fun migrateHabitEntries(userId: String) {
        val entries = database.habitEntryDao().getAll()
        if (entries.isEmpty()) return
        // Batch in chunks of 500 to avoid request size limits
        entries.chunked(500).forEach { chunk ->
            val dtos = chunk.map { it.toDomain().toDto(userId) }
            supabase.postgrest["habit_entries"].upsert(dtos)
        }
    }

    private suspend fun migrateLabels(userId: String) {
        val labels = database.labelDao().getAll()
        if (labels.isEmpty()) return
        val dtos = labels.map { it.toDomain().toDto(userId) }
        supabase.postgrest["labels"].upsert(dtos)
    }

    private suspend fun migrateTransactions(userId: String) {
        val transactions = database.transactionDao().getAll()
        if (transactions.isEmpty()) return
        transactions.chunked(500).forEach { chunk ->
            val dtos = chunk.map { it.toDomain().toDto(userId) }
            supabase.postgrest["transactions"].upsert(dtos)
        }
    }

    private suspend fun migrateEmotionalContext(userId: String) {
        val contexts = database.emotionalContextDao().getAll()
        if (contexts.isEmpty()) return
        val dtos = contexts.map { it.toDomain().toDto(userId) }
        supabase.postgrest["emotional_context"].upsert(dtos)
    }

    companion object {
        private const val TAG = "RoomMigration"
        const val WORK_NAME = "room_to_supabase_migration"
    }
}
