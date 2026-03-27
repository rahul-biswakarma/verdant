package com.verdant.core.database.usecase

import com.verdant.core.database.dao.StreakCacheDao
import com.verdant.core.database.entity.StreakCacheEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the streak cache table for fast streak lookups.
 * Avoids recomputing streaks from the full entry history on every read.
 */
@Singleton
class StreakCacheManager @Inject constructor(
    private val streakCacheDao: StreakCacheDao,
    private val calculateStreak: CalculateStreakUseCase,
) {
    private companion object {
        const val CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours
    }

    /**
     * Gets the cached streak for a habit. If the cache is stale or missing,
     * recomputes and caches the result.
     */
    suspend fun getCurrentStreak(habitId: String): Int {
        val cached = streakCacheDao.getByHabitId(habitId)
        if (cached != null && !isStale(cached)) {
            return cached.currentStreak
        }
        return recompute(habitId).currentStreak
    }

    suspend fun getLongestStreak(habitId: String): Int {
        val cached = streakCacheDao.getByHabitId(habitId)
        if (cached != null && !isStale(cached)) {
            return cached.longestStreak
        }
        return recompute(habitId).longestStreak
    }

    suspend fun getCompletionRate(habitId: String): Float {
        val cached = streakCacheDao.getByHabitId(habitId)
        if (cached != null && !isStale(cached)) {
            return cached.completionRate
        }
        return recompute(habitId).completionRate
    }

    /** Invalidates cache for a habit (call after new entry logged). */
    suspend fun invalidate(habitId: String) {
        streakCacheDao.invalidate(habitId)
    }

    /** Invalidates all cached streaks. */
    suspend fun invalidateAll() {
        streakCacheDao.deleteAll()
    }

    /** Batch get cached streaks, recomputing any that are stale. */
    suspend fun getCurrentStreaks(habitIds: List<String>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val allCached = streakCacheDao.getAll()
        val cacheMap = allCached.associateBy { it.habitId }

        for (id in habitIds) {
            val cached = cacheMap[id]
            if (cached != null && !isStale(cached)) {
                result[id] = cached.currentStreak
            } else {
                result[id] = recompute(id).currentStreak
            }
        }
        return result
    }

    private fun isStale(cache: StreakCacheEntity): Boolean =
        System.currentTimeMillis() - cache.cachedAt > CACHE_TTL_MS

    private suspend fun recompute(habitId: String): StreakCacheEntity {
        val current = calculateStreak.currentStreak(habitId)
        val longest = calculateStreak.longestStreak(habitId)
        val rate = calculateStreak.completionRate(habitId)

        val entity = StreakCacheEntity(
            habitId = habitId,
            currentStreak = current,
            longestStreak = longest,
            completionRate = rate,
            cachedAt = System.currentTimeMillis(),
        )
        streakCacheDao.upsert(entity)
        return entity
    }
}
