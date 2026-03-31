package com.verdant.core.supabase.di

import com.verdant.core.model.repository.AchievementRepository
import com.verdant.core.model.repository.ActivityRecordRepository
import com.verdant.core.model.repository.BudgetRepository
import com.verdant.core.model.repository.CrossCorrelationRepository
import com.verdant.core.genui.generation.DashboardLayoutGenerator
import com.verdant.core.model.repository.DashboardLayoutRepository
import com.verdant.core.model.repository.DeviceSignalRepository
import com.verdant.core.model.repository.DeviceStatRepository
import com.verdant.core.model.repository.EmotionalContextRepository
import com.verdant.core.model.repository.HabitEntryRepository
import com.verdant.core.model.repository.HabitRepository
import com.verdant.core.model.repository.HabitTargetHistoryRepository
import com.verdant.core.model.repository.HealthRecordRepository
import com.verdant.core.model.repository.LabelRepository
import com.verdant.core.model.repository.LifeScoreRepository
import com.verdant.core.model.repository.MerchantMappingRepository
import com.verdant.core.model.repository.PendingAIRequestRepository
import com.verdant.core.model.repository.PlayerProfileRepository
import com.verdant.core.model.repository.PredictionRepository
import com.verdant.core.model.repository.QuestRepository
import com.verdant.core.model.repository.RecurringTransactionRepository
import com.verdant.core.model.repository.StreakCacheRepository
import com.verdant.core.model.repository.StoryEventRepository
import com.verdant.core.model.repository.StoryRepository
import com.verdant.core.model.repository.TransactionRepository
import com.verdant.core.model.repository.WeatherRepository
import com.verdant.core.supabase.repository.AchievementSupabaseRepository
import com.verdant.core.supabase.repository.ActivityRecordSupabaseRepository
import com.verdant.core.supabase.repository.BudgetSupabaseRepository
import com.verdant.core.supabase.repository.CrossCorrelationSupabaseRepository
import com.verdant.core.supabase.repository.DashboardLayoutSupabaseRepository
import com.verdant.core.supabase.SupabaseDashboardLayoutGenerator
import com.verdant.core.supabase.repository.DeviceSignalSupabaseRepository
import com.verdant.core.supabase.repository.DeviceStatSupabaseRepository
import com.verdant.core.supabase.repository.EmotionalContextSupabaseRepository
import com.verdant.core.supabase.repository.HabitEntrySupabaseRepository
import com.verdant.core.supabase.repository.HabitSupabaseRepository
import com.verdant.core.supabase.repository.HabitTargetHistorySupabaseRepository
import com.verdant.core.supabase.repository.HealthRecordSupabaseRepository
import com.verdant.core.supabase.repository.LabelSupabaseRepository
import com.verdant.core.supabase.repository.LifeScoreSupabaseRepository
import com.verdant.core.supabase.repository.MerchantMappingSupabaseRepository
import com.verdant.core.supabase.repository.PendingAIRequestSupabaseRepository
import com.verdant.core.supabase.repository.PlayerProfileSupabaseRepository
import com.verdant.core.supabase.repository.PredictionSupabaseRepository
import com.verdant.core.supabase.repository.QuestSupabaseRepository
import com.verdant.core.supabase.repository.RecurringTransactionSupabaseRepository
import com.verdant.core.supabase.repository.StreakCacheSupabaseRepository
import com.verdant.core.supabase.repository.StoryEventSupabaseRepository
import com.verdant.core.supabase.repository.StorySupabaseRepository
import com.verdant.core.supabase.repository.TransactionSupabaseRepository
import com.verdant.core.supabase.repository.WeatherSupabaseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SupabaseRepositoryModule {

    // --- Batch 1 (already migrated) ---

    @Binds @Singleton
    abstract fun bindHabitRepository(impl: HabitSupabaseRepository): HabitRepository

    @Binds @Singleton
    abstract fun bindHabitEntryRepository(impl: HabitEntrySupabaseRepository): HabitEntryRepository

    @Binds @Singleton
    abstract fun bindLabelRepository(impl: LabelSupabaseRepository): LabelRepository

    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: TransactionSupabaseRepository): TransactionRepository

    @Binds @Singleton
    abstract fun bindEmotionalContextRepository(impl: EmotionalContextSupabaseRepository): EmotionalContextRepository

    // --- Batch 2-5 (newly migrated) ---

    @Binds @Singleton
    abstract fun bindBudgetRepository(impl: BudgetSupabaseRepository): BudgetRepository

    @Binds @Singleton
    abstract fun bindRecurringTransactionRepository(impl: RecurringTransactionSupabaseRepository): RecurringTransactionRepository

    @Binds @Singleton
    abstract fun bindPlayerProfileRepository(impl: PlayerProfileSupabaseRepository): PlayerProfileRepository

    @Binds @Singleton
    abstract fun bindQuestRepository(impl: QuestSupabaseRepository): QuestRepository

    @Binds @Singleton
    abstract fun bindPredictionRepository(impl: PredictionSupabaseRepository): PredictionRepository

    @Binds @Singleton
    abstract fun bindLifeScoreRepository(impl: LifeScoreSupabaseRepository): LifeScoreRepository

    @Binds @Singleton
    abstract fun bindHealthRecordRepository(impl: HealthRecordSupabaseRepository): HealthRecordRepository

    @Binds @Singleton
    abstract fun bindDeviceStatRepository(impl: DeviceStatSupabaseRepository): DeviceStatRepository

    // --- Signal-related repos ---

    @Binds @Singleton
    abstract fun bindAchievementRepository(impl: AchievementSupabaseRepository): AchievementRepository

    @Binds @Singleton
    abstract fun bindActivityRecordRepository(impl: ActivityRecordSupabaseRepository): ActivityRecordRepository

    @Binds @Singleton
    abstract fun bindCrossCorrelationRepository(impl: CrossCorrelationSupabaseRepository): CrossCorrelationRepository

    @Binds @Singleton
    abstract fun bindDeviceSignalRepository(impl: DeviceSignalSupabaseRepository): DeviceSignalRepository

    @Binds @Singleton
    abstract fun bindHabitTargetHistoryRepository(impl: HabitTargetHistorySupabaseRepository): HabitTargetHistoryRepository

    @Binds @Singleton
    abstract fun bindWeatherRepository(impl: WeatherSupabaseRepository): WeatherRepository

    @Binds @Singleton
    abstract fun bindMerchantMappingRepository(impl: MerchantMappingSupabaseRepository): MerchantMappingRepository

    @Binds @Singleton
    abstract fun bindPendingAIRequestRepository(impl: PendingAIRequestSupabaseRepository): PendingAIRequestRepository

    @Binds @Singleton
    abstract fun bindStreakCacheRepository(impl: StreakCacheSupabaseRepository): StreakCacheRepository

    // --- Stories ---

    @Binds @Singleton
    abstract fun bindStoryRepository(impl: StorySupabaseRepository): StoryRepository

    @Binds @Singleton
    abstract fun bindStoryEventRepository(impl: StoryEventSupabaseRepository): StoryEventRepository

    // --- Gen UI ---

    @Binds @Singleton
    abstract fun bindDashboardLayoutRepository(impl: DashboardLayoutSupabaseRepository): DashboardLayoutRepository

    @Binds @Singleton
    abstract fun bindDashboardLayoutGenerator(impl: SupabaseDashboardLayoutGenerator): DashboardLayoutGenerator
}
