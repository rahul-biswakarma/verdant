package com.verdant.core.health.di

import com.verdant.core.health.HealthConnectClient
import com.verdant.core.health.HealthConnectClientImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {

    @Binds
    @Singleton
    abstract fun bindHealthConnectClient(impl: HealthConnectClientImpl): HealthConnectClient
}
