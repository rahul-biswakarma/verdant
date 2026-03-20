package com.verdant.core.network

/**
 * Typed exceptions thrown by [VerdantApiService] and [CloudAI].
 *
 * These are the only error types callers need to handle; all lower-level
 * networking and HTTP errors are mapped here before they escape the network layer.
 */
sealed class VerdantApiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * The device has no active internet connection, or the request timed out
     * before reaching the server.
     */
    class NetworkException(cause: Throwable? = null) :
        VerdantApiException("Network unavailable — check your internet connection.", cause)

    /**
     * The user has exceeded their daily request quota.
     *
     * @param retryAfterSeconds Server-hinted seconds until the quota resets, if provided.
     */
    class RateLimitException(val retryAfterSeconds: Int? = null) :
        VerdantApiException(
            "Daily AI request limit reached. " +
                (retryAfterSeconds?.let { "Try again in ${it}s." } ?: "Try again tomorrow."),
        )

    /**
     * The Firebase Auth token was missing, expired, or rejected by the backend.
     * The caller should trigger a sign-in flow.
     */
    class AuthException(message: String = "Authentication failed. Please sign in again.") :
        VerdantApiException(message)

    /**
     * An unexpected HTTP error was returned by the server.
     *
     * @param code  HTTP status code (e.g. 500).
     */
    class ServerException(val code: Int, message: String? = null) :
        VerdantApiException("Server error $code${message?.let { ": $it" } ?: "."}")
}
