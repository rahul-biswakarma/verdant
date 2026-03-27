package com.verdant.core.emotional

import com.verdant.core.model.EmotionalState

class EmotionalStateMachine {

    data class StateInput(
        val completionRateLast7d: Float,
        val completionRateLast14d: Float,
        val screenTimeChangePercent: Float,
        val sleepConsistency: Float,
        val dimensionsImproving: Int,
        val dimensionsDeclining: Int,
        val daysSinceSignificantDrop: Int,
    )

    fun computeState(input: StateInput, currentState: EmotionalState?): EmotionalState {
        // CRISIS: multiple dimensions crashing simultaneously
        if (input.dimensionsDeclining >= 3 && input.completionRateLast7d < 0.3f) {
            return EmotionalState.CRISIS
        }

        // SURGE: sudden spike in all metrics
        if (input.dimensionsImproving >= 4 && input.completionRateLast7d > 0.9f) {
            return EmotionalState.SURGE
        }

        // RECOVERY: upward trend after CRISIS/DRIFT
        if (currentState == EmotionalState.CRISIS || currentState == EmotionalState.DRIFT) {
            if (input.completionRateLast7d > input.completionRateLast14d + 0.1f) {
                return EmotionalState.RECOVERY
            }
        }

        // FLOW: high completion + low screen time + consistent sleep
        if (input.completionRateLast7d > 0.8f &&
            input.screenTimeChangePercent < 0.1f &&
            input.sleepConsistency > 0.7f
        ) {
            return EmotionalState.FLOW
        }

        // DRIFT: gradual decline over 7+ days
        if (input.completionRateLast7d < input.completionRateLast14d - 0.15f &&
            input.dimensionsDeclining >= 2
        ) {
            return EmotionalState.DRIFT
        }

        // PLATEAU: consistent but flat
        if (kotlin.math.abs(input.completionRateLast7d - input.completionRateLast14d) < 0.05f &&
            input.daysSinceSignificantDrop > 14
        ) {
            return EmotionalState.PLATEAU
        }

        return currentState ?: EmotionalState.FLOW
    }
}
