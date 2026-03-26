package com.verdant.core.database.di

import android.content.Context
import androidx.room.Room
import com.verdant.core.database.VerdantDatabase
import com.verdant.core.database.dao.AIInsightDao
import com.verdant.core.database.dao.HabitDao
import com.verdant.core.database.dao.HabitEntryDao
import com.verdant.core.database.dao.LabelDao
import com.verdant.core.database.dao.MerchantMappingDao
import com.verdant.core.database.dao.TransactionDao
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
    )
        .addMigrations(
            VerdantDatabase.MIGRATION_1_2,
            VerdantDatabase.MIGRATION_2_3,
            VerdantDatabase.MIGRATION_3_4,
        )
        .build()

    @Provides
    fun provideHabitDao(db: VerdantDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitEntryDao(db: VerdantDatabase): HabitEntryDao = db.habitEntryDao()

    @Provides
    fun provideLabelDao(db: VerdantDatabase): LabelDao = db.labelDao()

    @Provides
    fun provideAIInsightDao(db: VerdantDatabase): AIInsightDao = db.aiInsightDao()

    @Provides
    fun provideTransactionDao(db: VerdantDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideMerchantMappingDao(db: VerdantDatabase): MerchantMappingDao = db.merchantMappingDao()
}
