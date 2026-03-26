package com.verdant.core.common

import com.verdant.core.model.DayCell
import com.verdant.core.model.HabitEntry
import java.time.LocalDate

object DayCellBuilder {

    fun buildCells(
        entries: List<HabitEntry>,
        start: LocalDate,
        end: LocalDate,
    ): List<DayCell> {
        val entryMap = entries.associateBy { it.date }
        val days = start.datesUntil(end.plusDays(1)).toList()
        return days.map { date ->
            val entry = entryMap[date]
            DayCell(
                date = date,
                intensity = when {
                    entry == null -> 0f
                    entry.skipped -> 0f
                    entry.completed -> 1f
                    entry.value != null -> 0.5f
                    else -> 0f
                },
                entryCount = if (entry != null) 1 else 0,
                completedCount = if (entry?.completed == true) 1 else 0,
            )
        }
    }
}
