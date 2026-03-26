package com.verdant.core.database.di

import android.content.Context
import androidx.room.Room
import com.verdant.core.database.VerdantDatabase
import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.migration.MIGRATION_1_2
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.dao.HabitEntryDao
import com.verdant.core.database.dao.LabelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVerdantDatabase(
        @ApplicationContext context: Context,
    ): VerdantDatabase = Room.databaseBuilder(
        context,
        VerdantDatabase::class.java,
        "verdant.db",
    ).addMigrations(MIGRATION_1_2).build()

    @Provides
    fun provideHabitDao(db: VerdantDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitEntryDao(db: VerdantDatabase): HabitEntryDao = db.habitEntryDao()

    @Provides
    fun provideLabelDao(db: VerdantDatabase): LabelDao = db.labelDao()

    @Provides
    fun provideAIInsightDao(db: VerdantDatabase): AIInsightDao = db.aiInsightDao()
}
