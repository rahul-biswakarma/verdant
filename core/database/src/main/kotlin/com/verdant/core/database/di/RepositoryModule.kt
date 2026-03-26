package com.verdant.core.database.di

import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitEntryRepositoryImpl
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.repository.HabitRepositoryImpl
import com.verdant.core.database.repository.LabelRepository
import com.verdant.core.database.repository.LabelRepositoryImpl
import com.verdant.core.database.repository.TransactionRepository
import com.verdant.core.database.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    @Singleton
    abstract fun bindLabelRepository(impl: LabelRepositoryImpl): LabelRepository

    @Binds
    @Singleton
    abstract fun bindHabitEntryRepository(impl: HabitEntryRepositoryImpl): HabitEntryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
}
