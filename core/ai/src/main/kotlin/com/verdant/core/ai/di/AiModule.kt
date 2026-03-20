package com.verdant.core.ai.di

import com.verdant.core.ai.VerdantAI
import com.verdant.core.ai.VerdantAIRouter
import com.verdant.core.ai.habit.FallbackHabitParser
import com.verdant.core.ai.habit.GeminiNanoHabitParser
import com.verdant.core.ai.habit.HabitParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    /**
     * Legacy [HabitParser] binding — kept for backward compatibility.
     * New code should inject [VerdantAI] and call [VerdantAI.parseHabitDescription].
     */
    @Binds
    @Singleton
    abstract fun bindHabitParser(impl: GeminiNanoHabitParser): HabitParser

    /**
     * Binds the main [VerdantAI] interface to [VerdantAIRouter], which
     * automatically selects between on-device (Gemini Nano), template fallback,
     * and Claude cloud backends based on network availability and operation type.
     */
    @Binds
    @Singleton
    abstract fun bindVerdantAI(impl: VerdantAIRouter): VerdantAI
}
