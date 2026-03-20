package com.verdant.core.common

import java.time.LocalDate
import java.time.ZoneId

object DateUtils {
    fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())

    fun LocalDate.toEpochMilli(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
