package com.verdant.core.social.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Social module — SocialRepository is constructor-injected with SupabaseClient
 * from SupabaseModule in :core:supabase.
 */
@Module
@InstallIn(SingletonComponent::class)
object SocialModule
