package com.verdant.core.sync.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Sync module — DeviceSyncManager and SignalPublisher are constructor-injected
 * singletons; no manual @Provides needed. The SupabaseClient comes from
 * SupabaseModule in :core:supabase.
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule
