package com.verdant.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.verdant.core.database.converter.Converters
import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.dao.HabitEntryDao
import com.verdant.core.database.dao.LabelDao
import com.verdant.core.database.entity.AIInsightEntity
import com.verdant.core.database.entity.HabitEntity
import com.verdant.core.database.entity.HabitEntryEntity
import com.verdant.core.database.entity.LabelEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitEntryEntity::class,
        LabelEntity::class,
        AIInsightEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class VerdantDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitEntryDao(): HabitEntryDao
    abstract fun labelDao(): LabelDao
    abstract fun aiInsightDao(): AIInsightDao
}
