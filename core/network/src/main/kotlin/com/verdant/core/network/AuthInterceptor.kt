package com.verdant.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that attaches the current Supabase Auth access token as a
 * `Authorization: Bearer <token>` header on every outgoing request.
 *
 * If no user is signed in, the request is forwarded without the header.
 * The backend will respond with 401 in that case, which [VerdantApiService]
 * maps to [VerdantApiException.AuthException].
 */
class AuthInterceptor @Inject constructor(
    private val supabase: SupabaseClient,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = supabase.auth.currentAccessTokenOrNull()

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
