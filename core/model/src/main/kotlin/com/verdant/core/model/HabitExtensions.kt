package com.verdant.core.model

import java.time.LocalDate

/**
 * Returns true if this habit is scheduled to occur on [date].
 *
 * scheduleDays bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
 * DayOfWeek.value: MONDAY=1 … SUNDAY=7
 */
fun Habit.isScheduledForDate(date: LocalDate): Boolean = when (frequency) {
    HabitFrequency.DAILY -> true
    HabitFrequency.WEEKDAYS -> date.dayOfWeek.value in 1..5
    HabitFrequency.WEEKENDS -> date.dayOfWeek.value in 6..7
    HabitFrequency.SPECIFIC_DAYS -> {
        val bit = 1 shl (date.dayOfWeek.value - 1)
        scheduleDays and bit != 0
    }
    HabitFrequency.TIMES_PER_WEEK -> true
}
