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

// ── Qualifiers ────────────────────────────────────────────────────────────────

/** Tags the Retrofit instance / OkHttpClient that targets the Anthropic API directly. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnthropicRetrofit

/**
 * Tags the Retrofit instance / OkHttpClient that targets the Verdant Firebase
 * Functions backend proxy.
 *
 * Before deploying, replace [FIREBASE_FUNCTIONS_BASE_URL] with your project URL:
 *   https://us-central1-{YOUR_PROJECT_ID}.cloudfunctions.net/
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FirebaseFunctionsRetrofit

// ── Module ────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // TODO: Replace with your deployed Firebase Functions URL before release.
    private const val FIREBASE_FUNCTIONS_BASE_URL =
        "https://us-central1-verdant-app.cloudfunctions.net/"

    private const val ANTHROPIC_BASE_URL = "https://api.anthropic.com/v1/"

    // ── Shared ────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    // ── Anthropic (direct API, API-key auth) ──────────────────────────────────

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

    /** Legacy direct Claude client kept for optional internal use. */
    @Provides
    @Singleton
    fun provideClaudeApiService(
        @AnthropicRetrofit retrofit: Retrofit,
    ): ClaudeApiService = retrofit.create(ClaudeApiService::class.java)

    // ── Firebase Functions (proxy, Firebase Auth token auth) ──────────────────

    @Provides
    @Singleton
    @FirebaseFunctionsRetrofit
    fun provideFirebaseFunctionsOkHttpClient(
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
    @FirebaseFunctionsRetrofit
    fun provideFirebaseFunctionsRetrofit(
        @FirebaseFunctionsRetrofit okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(FIREBASE_FUNCTIONS_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideVerdantApiService(
        @FirebaseFunctionsRetrofit retrofit: Retrofit,
    ): VerdantApiService = retrofit.create(VerdantApiService::class.java)
}
