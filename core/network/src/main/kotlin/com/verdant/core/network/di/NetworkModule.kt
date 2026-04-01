package com.verdant.core.network.di

import com.verdant.core.network.AuthInterceptor
import com.verdant.core.network.ClaudeApiService
import com.verdant.core.network.VerdantApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton


/** Tags the Retrofit instance / OkHttpClient that targets the Anthropic API directly. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnthropicRetrofit

/**
 * Tags the Retrofit instance / OkHttpClient that targets the Verdant Supabase
 * Edge Functions backend.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SupabaseEdgeFunctionsRetrofit


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Base URL for Supabase Edge Functions.
     * Endpoints are invoked as POST {BASE_URL}/{functionName}.
     */
    private const val SUPABASE_FUNCTIONS_BASE_URL =
        "https://iktudbhdorbnmniwikhm.supabase.co/functions/v1/"

    private const val ANTHROPIC_BASE_URL = "https://api.anthropic.com/v1/"


    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }


    @Provides
    @Singleton
    @AnthropicRetrofit
    fun provideAnthropicOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            },
        )
        .build()

    @Provides
    @Singleton
    @AnthropicRetrofit
    fun provideAnthropicRetrofit(
        @AnthropicRetrofit okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ANTHROPIC_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideClaudeApiService(
        @AnthropicRetrofit retrofit: Retrofit,
    ): ClaudeApiService = retrofit.create(ClaudeApiService::class.java)


    @Provides
    @Singleton
    @SupabaseEdgeFunctionsRetrofit
    fun provideSupabaseEdgeFunctionsOkHttpClient(
        authInterceptor: AuthInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            },
        )
        .build()

    @Provides
    @Singleton
    @SupabaseEdgeFunctionsRetrofit
    fun provideSupabaseEdgeFunctionsRetrofit(
        @SupabaseEdgeFunctionsRetrofit okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(SUPABASE_FUNCTIONS_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideVerdantApiService(
        @SupabaseEdgeFunctionsRetrofit retrofit: Retrofit,
    ): VerdantApiService = retrofit.create(VerdantApiService::class.java)
}
