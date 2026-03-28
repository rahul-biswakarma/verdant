package com.verdant.core.emotional

import com.verdant.core.model.EmotionalState
import org.junit.Assert.assertEquals
import org.junit.Test

class EmotionalStateMachineTest {

    private val machine = EmotionalStateMachine()

    @Test
    fun `high completion and stability yields FLOW`() {
        val result = machine.computeState(
            EmotionalStateMachine.StateInput(
                completionRateLast7d = 0.85f,
                completionRateLast14d = 0.82f,
                screenTimeChangePercent = 0.05f,
                sleepConsistency = 0.8f,
                dimensionsImproving = 2,
                dimensionsDeclining = 0,
                daysSinceSignificantDrop = 30,
            ),
            currentState = null,
        )
        assertEquals(EmotionalState.FLOW, result)
    }

    @Test
    fun `many declining dimensions with low completion yields CRISIS`() {
        val result = machine.computeState(
            EmotionalStateMachine.StateInput(
                completionRateLast7d = 0.2f,
                completionRateLast14d = 0.5f,
                screenTimeChangePercent = 0.3f,
                sleepConsistency = 0.3f,
                dimensionsImproving = 0,
                dimensionsDeclining = 4,
                daysSinceSignificantDrop = 2,
            ),
            currentState = null,
        )
        assertEquals(EmotionalState.CRISIS, result)
    }

    @Test
    fun `all dimensions improving with high completion yields SURGE`() {
        val result = machine.computeState(
            EmotionalStateMachine.StateInput(
                completionRateLast7d = 0.95f,
                completionRateLast14d = 0.6f,
                screenTimeChangePercent = -0.2f,
                sleepConsistency = 0.9f,
                dimensionsImproving = 5,
                dimensionsDeclining = 0,
                daysSinceSignificantDrop = 20,
            ),
            currentState = null,
        )
        assertEquals(EmotionalState.SURGE, result)
    }

    @Test
    fun `upward trend after CRISIS yields RECOVERY`() {
        val result = machine.computeState(
            EmotionalStateMachine.StateInput(
                completionRateLast7d = 0.5f,
                completionRateLast14d = 0.3f,
                screenTimeChangePercent = 0.0f,
                sleepConsistency = 0.5f,
                dimensionsImproving = 2,
                dimensionsDeclining = 1,
                daysSinceSignificantDrop = 5,
            ),
            currentState = EmotionalState.CRISIS,
        )
        assertEquals(EmotionalState.RECOVERY, result)
    }

    @Test
    fun `declining completion with dimensions down yields DRIFT`() {
        val result = machine.computeState(
            EmotionalStateMachine.StateInput(
                completionRateLast7d = 0.4f,
                completionRateLast14d = 0.6f,
                screenTimeChangePercent = 0.2f,
                sleepConsistency = 0.5f,
                dimensionsImproving = 0,
                dimensionsDeclining = 2,
                daysSinceSignificantDrop = 10,
            ),
            currentState = EmotionalState.FLOW,
        )
        assertEquals(EmotionalState.DRIFT, result)
    }

    @Test
    fun `flat performance for extended period yields PLATEAU`() {
        val result = machine.computeState(
            EmotionalStateMachine.StateInput(
                completionRateLast7d = 0.65f,
                completionRateLast14d = 0.63f,
                screenTimeChangePercent = 0.02f,
                sleepConsistency = 0.6f,
                dimensionsImproving = 1,
                dimensionsDeclining = 1,
                daysSinceSignificantDrop = 21,
            ),
            currentState = EmotionalState.FLOW,
        )
        assertEquals(EmotionalState.PLATEAU, result)
    }
}
