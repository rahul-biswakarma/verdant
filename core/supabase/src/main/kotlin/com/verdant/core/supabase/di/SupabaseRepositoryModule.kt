package com.verdant.core.supabase.di

import com.verdant.core.database.repository.EmotionalContextRepository
import com.verdant.core.database.repository.HabitEntryRepository
import com.verdant.core.database.repository.HabitRepository
import com.verdant.core.database.repository.LabelRepository
import com.verdant.core.database.repository.TransactionRepository
import com.verdant.core.supabase.repository.EmotionalContextSupabaseRepository
import com.verdant.core.supabase.repository.HabitEntrySupabaseRepository
import com.verdant.core.supabase.repository.HabitSupabaseRepository
import com.verdant.core.supabase.repository.LabelSupabaseRepository
import com.verdant.core.supabase.repository.TransactionSupabaseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SupabaseRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitSupabaseRepository): HabitRepository

    @Binds
    @Singleton
    abstract fun bindHabitEntryRepository(impl: HabitEntrySupabaseRepository): HabitEntryRepository

    @Binds
    @Singleton
    abstract fun bindLabelRepository(impl: LabelSupabaseRepository): LabelRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionSupabaseRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindEmotionalContextRepository(impl: EmotionalContextSupabaseRepository): EmotionalContextRepository
}
