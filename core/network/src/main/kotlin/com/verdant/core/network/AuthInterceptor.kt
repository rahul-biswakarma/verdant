package com.verdant.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * OkHttp interceptor that attaches the current Firebase Auth ID token as a
 * `Authorization: Bearer <token>` header on every outgoing request.
 *
 * If no user is signed in, or fetching the token fails, the request is forwarded
 * without the header. The backend will respond with 401 in that case, which
 * [VerdantApiService] maps to [VerdantApiException.AuthException].
 *
 * Token refresh is handled by Firebase: `getIdToken(false)` returns the cached
 * token if it is still valid (expires every hour), and fetches a new one
 * automatically when needed.
 *
 * This interceptor runs on OkHttp's background thread pool — blocking via
 * [Tasks.await] is intentional and safe here.
 */
class AuthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = fetchIdToken()

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }

    private fun fetchIdToken(): String? {
        return try {
            val user = FirebaseAuth.getInstance().currentUser ?: return null
            Tasks.await(user.getIdToken(/* forceRefresh = */ false), TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .token
        } catch (e: Exception) {
            // Log in debug builds; don't crash — the backend will reject the request if auth
            // is truly required, and the caller maps 401 → AuthException.
            null
        }
    }

    private companion object {
        const val TOKEN_TIMEOUT_SECONDS = 5L
    }
}
