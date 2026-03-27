package com.verdant.core.ai

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Circuit breaker for cloud AI calls. Prevents cascading failures when
 * Claude is rate-limited or experiencing outages.
 *
 * States:
 * - CLOSED: Normal operation, calls go through
 * - OPEN: All calls short-circuited to fallback for [OPEN_DURATION_MS]
 * - HALF_OPEN: One probe call allowed to test recovery
 */
@Singleton
class CloudAICircuitBreaker @Inject constructor() {

    enum class State { CLOSED, OPEN, HALF_OPEN }

    private val failureCount = AtomicInteger(0)
    private val openedAt = AtomicLong(0)
    private val lastFailureAt = AtomicLong(0)

    val state: State
        get() {
            val opened = openedAt.get()
            if (opened == 0L) return State.CLOSED

            val elapsed = System.currentTimeMillis() - opened
            return when {
                elapsed < OPEN_DURATION_MS -> State.OPEN
                elapsed < OPEN_DURATION_MS + HALF_OPEN_PROBE_WINDOW_MS -> State.HALF_OPEN
                else -> {
                    // Probe window expired without failure — reset to CLOSED
                    reset()
                    State.CLOSED
                }
            }
        }

    /** Returns true if the call should be allowed through. */
    fun shouldAllow(): Boolean = when (state) {
        State.CLOSED -> true
        State.OPEN -> false
        State.HALF_OPEN -> true // Allow one probe call
    }

    /** Record a successful call. Resets the circuit breaker. */
    fun recordSuccess() {
        reset()
    }

    /** Record a failed call. Opens the circuit after [FAILURE_THRESHOLD] consecutive failures. */
    fun recordFailure() {
        val now = System.currentTimeMillis()
        val lastFail = lastFailureAt.get()

        // Reset failure count if the last failure was more than [FAILURE_WINDOW_MS] ago
        if (now - lastFail > FAILURE_WINDOW_MS) {
            failureCount.set(0)
        }

        lastFailureAt.set(now)
        val count = failureCount.incrementAndGet()

        if (count >= FAILURE_THRESHOLD) {
            openedAt.set(now)
        }
    }

    fun reset() {
        failureCount.set(0)
        openedAt.set(0)
        lastFailureAt.set(0)
    }

    companion object {
        const val FAILURE_THRESHOLD = 3
        const val FAILURE_WINDOW_MS = 60 * 60 * 1000L // 1 hour
        const val OPEN_DURATION_MS = 2 * 60 * 60 * 1000L // 2 hours
        const val HALF_OPEN_PROBE_WINDOW_MS = 5 * 60 * 1000L // 5 min probe window
    }
}
