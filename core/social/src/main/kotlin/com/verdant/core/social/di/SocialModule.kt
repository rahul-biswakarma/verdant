package com.verdant.core.social.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Social module — FirebaseAuth is already provided by SyncModule,
 * so no duplicate binding here.
 */
@Module
@InstallIn(SingletonComponent::class)
object SocialModule
