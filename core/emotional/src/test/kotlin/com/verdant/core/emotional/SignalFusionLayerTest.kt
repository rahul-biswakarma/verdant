package com.verdant.core.emotional

import com.verdant.core.model.InferredMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalFusionLayerTest {

    private val layer = SignalFusionLayer()

    @Test
    fun `all positive signals yield ENERGIZED`() {
        val result = layer.fuse(
            SignalFusionLayer.MicroSignals(
                habitCompletionRate = 0.9f,
                screenTimeMinutes = 60.0,
                sleepHours = 8.0,
                exerciseMinutes = 30.0,
                spendingRatio = 0.8,
                notificationCount = 20,
                calendarBusyHours = 3.0,
            )
        )
        assertEquals(InferredMood.ENERGIZED, result.mood)
        assertTrue(result.energyLevel > 60)
    }

    @Test
    fun `all negative signals yield STRESSED`() {
        val result = layer.fuse(
            SignalFusionLayer.MicroSignals(
                habitCompletionRate = 0.1f,
                screenTimeMinutes = 400.0,
                sleepHours = 3.0,
                exerciseMinutes = 0.0,
                spendingRatio = 2.0,
                notificationCount = 150,
                calendarBusyHours = 10.0,
            )
        )
        assertEquals(InferredMood.STRESSED, result.mood)
    }

    @Test
    fun `poor sleep with negative signals yields ANXIOUS`() {
        val result = layer.fuse(
            SignalFusionLayer.MicroSignals(
                habitCompletionRate = 0.2f,
                screenTimeMinutes = 300.0,
                sleepHours = 4.0,
                exerciseMinutes = 0.0,
                spendingRatio = 1.0,
                notificationCount = 50,
                calendarBusyHours = 4.0,
            )
        )
        assertEquals(InferredMood.ANXIOUS, result.mood)
    }

    @Test
    fun `neutral signals yield NEUTRAL`() {
        val result = layer.fuse(
            SignalFusionLayer.MicroSignals(
                habitCompletionRate = 0.5f,
                screenTimeMinutes = 150.0,
                sleepHours = 6.5,
                exerciseMinutes = 10.0,
                spendingRatio = 1.0,
                notificationCount = 50,
                calendarBusyHours = 5.0,
            )
        )
        assertEquals(InferredMood.NEUTRAL, result.mood)
    }

    @Test
    fun `confidence increases with more data points`() {
        val noData = layer.fuse(
            SignalFusionLayer.MicroSignals(
                habitCompletionRate = 0.5f,
                screenTimeMinutes = 0.0,
                sleepHours = 0.0,
                exerciseMinutes = 0.0,
                spendingRatio = 0.0,
                notificationCount = 0,
                calendarBusyHours = 0.0,
            )
        )
        val fullData = layer.fuse(
            SignalFusionLayer.MicroSignals(
                habitCompletionRate = 0.5f,
                screenTimeMinutes = 100.0,
                sleepHours = 7.0,
                exerciseMinutes = 20.0,
                spendingRatio = 1.0,
                notificationCount = 30,
                calendarBusyHours = 4.0,
            )
        )
        assertTrue(fullData.confidence > noData.confidence)
        assertEquals(1.0f, fullData.confidence)
    }

    @Test
    fun `energy level is bounded 0-100`() {
        val extremeHigh = layer.fuse(
            SignalFusionLayer.MicroSignals(
                habitCompletionRate = 1.0f,
                screenTimeMinutes = 0.0,
                sleepHours = 12.0,
                exerciseMinutes = 120.0,
                spendingRatio = 0.0,
                notificationCount = 0,
                calendarBusyHours = 0.0,
            )
        )
        assertTrue(extremeHigh.energyLevel in 0..100)

        val extremeLow = layer.fuse(
            SignalFusionLayer.MicroSignals(
                habitCompletionRate = 0.0f,
                screenTimeMinutes = 600.0,
                sleepHours = 0.0,
                exerciseMinutes = 0.0,
                spendingRatio = 3.0,
                notificationCount = 200,
                calendarBusyHours = 12.0,
            )
        )
        assertTrue(extremeLow.energyLevel in 0..100)
    }
}
