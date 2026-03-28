package com.verdant.core.database.di

import com.verdant.core.database.repository.BudgetRepository
import com.verdant.core.database.repository.BudgetRepositoryImpl
import com.verdant.core.database.repository.DeviceStatRepository
import com.verdant.core.database.repository.DeviceStatRepositoryImpl
import com.verdant.core.database.repository.HealthRecordRepository
import com.verdant.core.database.repository.HealthRecordRepositoryImpl
import com.verdant.core.database.repository.LifeScoreRepository
import com.verdant.core.database.repository.LifeScoreRepositoryImpl
import com.verdant.core.database.repository.PlayerProfileRepository
import com.verdant.core.database.repository.PlayerProfileRepositoryImpl
import com.verdant.core.database.repository.PredictionRepository
import com.verdant.core.database.repository.PredictionRepositoryImpl
import com.verdant.core.database.repository.QuestRepository
import com.verdant.core.database.repository.QuestRepositoryImpl
import com.verdant.core.database.repository.RecurringTransactionRepository
import com.verdant.core.database.repository.RecurringTransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings for repositories still using Room.
 * Habit, HabitEntry, Label, Transaction, and EmotionalContext
 * have been migrated to Supabase (see core:supabase module).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindHealthRecordRepository(impl: HealthRecordRepositoryImpl): HealthRecordRepository

    @Binds @Singleton
    abstract fun bindDeviceStatRepository(impl: DeviceStatRepositoryImpl): DeviceStatRepository

    @Binds @Singleton
    abstract fun bindLifeScoreRepository(impl: LifeScoreRepositoryImpl): LifeScoreRepository

    @Binds @Singleton
    abstract fun bindPredictionRepository(impl: PredictionRepositoryImpl): PredictionRepository

    @Binds @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds @Singleton
    abstract fun bindRecurringTransactionRepository(impl: RecurringTransactionRepositoryImpl): RecurringTransactionRepository

    @Binds @Singleton
    abstract fun bindPlayerProfileRepository(impl: PlayerProfileRepositoryImpl): PlayerProfileRepository

    @Binds @Singleton
    abstract fun bindQuestRepository(impl: QuestRepositoryImpl): QuestRepository
}
