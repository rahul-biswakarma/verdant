package com.verdant.core.database.usecase

import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.model.HabitEntry
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Creates or updates a [HabitEntry] for the given habit on the given date.
 *
 * All log operations are upserts: if an entry already exists for the
 * (habitId, date) pair it is updated in-place; otherwise a new one is created.
 */
class LogEntryUseCase @Inject constructor(
    private val entryRepository: HabitEntryRepository,
) {

    /** Toggle binary completion. Passing null flips the current value. */
    suspend fun logBinary(
        habitId: String,
        date: LocalDate,
        completed: Boolean,
    ) {
        val now = System.currentTimeMillis()
        val existing = entryRepository.getByHabitAndDate(habitId, date)
        entryRepository.upsert(
            existing?.copy(
                completed = completed,
                skipped = false,
                updatedAt = now,
            ) ?: newEntry(habitId, date, now).copy(completed = completed),
        )
    }

    /** Add [delta] to the current quantitative value, completing if ≥ target. */
    suspend fun addQuantitative(
        habitId: String,
        date: LocalDate,
        delta: Double,
        target: Double?,
    ) {
        val now = System.currentTimeMillis()
        val existing = entryRepository.getByHabitAndDate(habitId, date)
        val newValue = (existing?.value ?: 0.0) + delta
        val completed = target != null && newValue >= target
        entryRepository.upsert(
            existing?.copy(value = newValue, completed = completed, updatedAt = now)
                ?: newEntry(habitId, date, now).copy(value = newValue, completed = completed),
        )
    }

    /** Set an absolute quantitative/duration/financial value directly. */
    suspend fun setQuantitative(
        habitId: String,
        date: LocalDate,
        value: Double,
        target: Double?,
    ) {
        val now = System.currentTimeMillis()
        val existing = entryRepository.getByHabitAndDate(habitId, date)
        val completed = target != null && value >= target
        entryRepository.upsert(
            existing?.copy(value = value, completed = completed, updatedAt = now)
                ?: newEntry(habitId, date, now).copy(value = value, completed = completed),
        )
    }

    /** Log a location check-in, optionally recording cumulative distance as value. */
    suspend fun logLocation(
        habitId: String,
        date: LocalDate,
        latitude: Double?,
        longitude: Double?,
        distanceKm: Double? = null,
    ) {
        val now = System.currentTimeMillis()
        val existing = entryRepository.getByHabitAndDate(habitId, date)
        entryRepository.upsert(
            existing?.copy(
                completed = true,
                latitude = latitude,
                longitude = longitude,
                value = distanceKm,
                updatedAt = now,
            ) ?: newEntry(habitId, date, now).copy(
                completed = true,
                latitude = latitude,
                longitude = longitude,
                value = distanceKm,
            ),
        )
    }

    /** Log a financial expense with an optional category tag. */
    suspend fun logFinancial(
        habitId: String,
        date: LocalDate,
        amount: Double,
        category: String?,
        budget: Double?,
    ) {
        val now = System.currentTimeMillis()
        val existing = entryRepository.getByHabitAndDate(habitId, date)
        val total = (existing?.value ?: 0.0) + amount
        val completed = budget == null || total >= budget
        entryRepository.upsert(
            existing?.copy(value = total, category = category, completed = completed, updatedAt = now)
                ?: newEntry(habitId, date, now).copy(value = total, category = category, completed = completed),
        )
    }

    /** Mark an entry as intentionally skipped. */
    suspend fun skip(habitId: String, date: LocalDate) {
        val now = System.currentTimeMillis()
        val existing = entryRepository.getByHabitAndDate(habitId, date)
        entryRepository.upsert(
            existing?.copy(skipped = true, completed = false, updatedAt = now)
                ?: newEntry(habitId, date, now).copy(skipped = true),
        )
    }

    /** Upsert a fully formed entry directly (used for retroactive editing). */
    suspend fun upsertEntry(entry: HabitEntry) = entryRepository.upsert(entry)

    private fun newEntry(habitId: String, date: LocalDate, now: Long) = HabitEntry(
        id = UUID.randomUUID().toString(),
        habitId = habitId,
        date = date,
        completed = false,
        value = null,
        latitude = null,
        longitude = null,
        note = null,
        category = null,
        skipped = false,
        createdAt = now,
        updatedAt = now,
    )
}
