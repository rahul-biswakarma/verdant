package com.verdant.core.database.converter

import androidx.room.TypeConverter
import com.verdant.core.model.HabitFrequency
import com.verdant.core.model.InsightType
import com.verdant.core.model.TrackingType
import com.verdant.core.model.VisualizationType
import java.time.LocalDate

class Converters {

    // LocalDate <-> Long (epoch day)

    @TypeConverter
    fun localDateToLong(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun longToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }

    // List<String> <-> comma-separated String

    @TypeConverter
    fun stringListToString(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun stringToStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(",")

    // Enum converters

    @TypeConverter
    fun trackingTypeToString(type: TrackingType): String = type.name

    @TypeConverter
    fun stringToTrackingType(value: String): TrackingType = TrackingType.valueOf(value)

    @TypeConverter
    fun habitFrequencyToString(freq: HabitFrequency): String = freq.name

    @TypeConverter
    fun stringToHabitFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)

    @TypeConverter
    fun insightTypeToString(type: InsightType): String = type.name

    @TypeConverter
    fun stringToInsightType(value: String): InsightType = InsightType.valueOf(value)

    @TypeConverter
    fun visualizationTypeToString(type: VisualizationType): String = type.name

    @TypeConverter
    fun stringToVisualizationType(value: String): VisualizationType =
        runCatching { VisualizationType.valueOf(value) }.getOrDefault(VisualizationType.PIXEL_GRID)
}
