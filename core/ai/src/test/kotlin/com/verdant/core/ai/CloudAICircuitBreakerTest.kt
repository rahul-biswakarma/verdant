package com.verdant.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloudAICircuitBreakerTest {

    private lateinit var breaker: CloudAICircuitBreaker

    @Before
    fun setUp() {
        breaker = CloudAICircuitBreaker()
    }

    @Test
    fun `initial state is CLOSED`() {
        assertEquals(CloudAICircuitBreaker.State.CLOSED, breaker.state)
    }

    @Test
    fun `shouldAllow returns true when CLOSED`() {
        assertTrue(breaker.shouldAllow())
    }

    @Test
    fun `single failure does not open circuit`() {
        breaker.recordFailure()
        assertEquals(CloudAICircuitBreaker.State.CLOSED, breaker.state)
        assertTrue(breaker.shouldAllow())
    }

    @Test
    fun `three consecutive failures open circuit`() {
        repeat(CloudAICircuitBreaker.FAILURE_THRESHOLD) {
            breaker.recordFailure()
        }
        assertEquals(CloudAICircuitBreaker.State.OPEN, breaker.state)
        assertFalse(breaker.shouldAllow())
    }

    @Test
    fun `success resets circuit to CLOSED`() {
        repeat(CloudAICircuitBreaker.FAILURE_THRESHOLD) {
            breaker.recordFailure()
        }
        assertEquals(CloudAICircuitBreaker.State.OPEN, breaker.state)

        breaker.recordSuccess()
        assertEquals(CloudAICircuitBreaker.State.CLOSED, breaker.state)
        assertTrue(breaker.shouldAllow())
    }

    @Test
    fun `reset clears all state`() {
        repeat(CloudAICircuitBreaker.FAILURE_THRESHOLD) {
            breaker.recordFailure()
        }
        breaker.reset()
        assertEquals(CloudAICircuitBreaker.State.CLOSED, breaker.state)
        assertTrue(breaker.shouldAllow())
    }

    @Test
    fun `two failures below threshold keep circuit closed`() {
        breaker.recordFailure()
        breaker.recordFailure()
        assertEquals(CloudAICircuitBreaker.State.CLOSED, breaker.state)
        assertTrue(breaker.shouldAllow())
    }
}
