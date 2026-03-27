package com.verdant.core.database.di

import android.content.Context
import androidx.room.Room
import com.verdant.core.database.VerdantDatabase
import com.verdant.core.database.dao.AchievementDao
import com.verdant.core.database.dao.ActivityRecordDao
import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.dao.BudgetDao
import com.verdant.core.database.dao.CrossCorrelationDao
import com.verdant.core.database.dao.DeviceSignalDao
import com.verdant.core.database.dao.DeviceStatDao
import com.verdant.core.database.dao.EmotionalContextDao
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.dao.HabitEntryDao
import com.verdant.core.database.dao.HabitTargetHistoryDao
import com.verdant.core.database.dao.HealthRecordDao
import com.verdant.core.database.dao.LabelDao
import com.verdant.core.database.dao.LifeScoreDao
import com.verdant.core.database.dao.PendingAIRequestDao
import com.verdant.core.database.dao.PlayerProfileDao
import com.verdant.core.database.dao.PredictionDao
import com.verdant.core.database.dao.QuestDao
import com.verdant.core.database.dao.RecurringTransactionDao
import com.verdant.core.database.dao.StreakCacheDao
import com.verdant.core.database.dao.WeatherDao
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
    ).addMigrations(
        VerdantDatabase.MIGRATION_1_2,
        VerdantDatabase.MIGRATION_2_3,
    ).build()

    @Provides fun provideHabitDao(db: VerdantDatabase): HabitDao = db.habitDao()
    @Provides fun provideHabitEntryDao(db: VerdantDatabase): HabitEntryDao = db.habitEntryDao()
    @Provides fun provideLabelDao(db: VerdantDatabase): LabelDao = db.labelDao()
    @Provides fun provideAIInsightDao(db: VerdantDatabase): AIInsightDao = db.aiInsightDao()
    @Provides fun provideHealthRecordDao(db: VerdantDatabase): HealthRecordDao = db.healthRecordDao()
    @Provides fun provideActivityRecordDao(db: VerdantDatabase): ActivityRecordDao = db.activityRecordDao()
    @Provides fun provideDeviceStatDao(db: VerdantDatabase): DeviceStatDao = db.deviceStatDao()
    @Provides fun provideWeatherDao(db: VerdantDatabase): WeatherDao = db.weatherDao()
    @Provides fun provideLifeScoreDao(db: VerdantDatabase): LifeScoreDao = db.lifeScoreDao()
    @Provides fun providePredictionDao(db: VerdantDatabase): PredictionDao = db.predictionDao()
    @Provides fun provideBudgetDao(db: VerdantDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideRecurringTransactionDao(db: VerdantDatabase): RecurringTransactionDao = db.recurringTransactionDao()
    @Provides fun provideEmotionalContextDao(db: VerdantDatabase): EmotionalContextDao = db.emotionalContextDao()
    @Provides fun providePlayerProfileDao(db: VerdantDatabase): PlayerProfileDao = db.playerProfileDao()
    @Provides fun provideQuestDao(db: VerdantDatabase): QuestDao = db.questDao()
    @Provides fun provideAchievementDao(db: VerdantDatabase): AchievementDao = db.achievementDao()
    @Provides fun provideDeviceSignalDao(db: VerdantDatabase): DeviceSignalDao = db.deviceSignalDao()
    @Provides fun provideCrossCorrelationDao(db: VerdantDatabase): CrossCorrelationDao = db.crossCorrelationDao()
    @Provides fun provideStreakCacheDao(db: VerdantDatabase): StreakCacheDao = db.streakCacheDao()
    @Provides fun provideHabitTargetHistoryDao(db: VerdantDatabase): HabitTargetHistoryDao = db.habitTargetHistoryDao()
    @Provides fun providePendingAIRequestDao(db: VerdantDatabase): PendingAIRequestDao = db.pendingAIRequestDao()
}
